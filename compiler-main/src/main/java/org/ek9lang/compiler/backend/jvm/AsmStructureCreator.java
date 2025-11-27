package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.JVMTypeNames.DESC_VOID_TO_VOID;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_ANY;
import static org.ek9lang.compiler.support.JVMTypeNames.FIELD_INSTANCE;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_OBJECT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_CLINIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_C_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_GET_INSTANCE;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_I_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_MAIN;

import java.util.Collections;
import java.util.List;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.data.ParameterDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.Field;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.SymbolGenus;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Designed to capture the ASM specifics for byte code generation.
 * Generates the actual program class from IR operations (not ek9.Main).
 */
final class AsmStructureCreator implements Opcodes {

  private final ConstructTargetTuple constructTargetTuple;
  private final INodeVisitor visitor;
  private final FullyQualifiedJvmName jvmNameConverter = new FullyQualifiedJvmName();
  private final JvmDescriptorConverter descriptorConverter = new JvmDescriptorConverter(jvmNameConverter);
  private final OperatorMap operatorMap = new OperatorMap();
  private ClassWriter classWriter;
  private ProgramEntryPointInstr programEntryPoint = null;

  AsmStructureCreator(final ConstructTargetTuple constructTargetTuple, final INodeVisitor visitor) {
    this.constructTargetTuple = constructTargetTuple;
    this.visitor = visitor;
  }

  void processClass() {
    if (constructTargetTuple.construct().isProgram()) {
      processProgram();
      return;
    }
    // Handle general classes (not programs)
    processGeneralClass();
  }

  private void processProgram() {
    final IRConstruct construct = constructTargetTuple.construct();

    // Extract PROGRAM_ENTRY_POINT_BLOCK to get parameter information
    storeProgramEntryPoint(construct);

    // Initialize the actual program class (not ek9.Main)
    initializeProgramClass(construct);

    // Generate methods from IR operations
    generateProgramMethodsFromIR(construct);

    // Finalize the program class
    classWriter.visitEnd();
  }

  /**
   * Process general class (not a program) bytecode generation.
   * Handles classes, records, traits, functions, and other aggregate types.
   * <p>
   * Functions are special: they require a singleton pattern with a static INSTANCE
   * field and getInstance() method to enable function calls via FUNCTION_INSTANCE IR instruction.
   * </p>
   */
  private void processGeneralClass() {
    final IRConstruct construct = constructTargetTuple.construct();

    // Initialize the class with proper superclass
    initializeGeneralClass(construct);

    // Generate field declarations
    generateFieldDeclarations(construct);

    // For concrete functions (FUNCTION genus): generate singleton pattern
    // Abstract function traits (FUNCTION_TRAIT genus) cannot be instantiated - no singleton
    if (construct.getSymbol().getGenus() == SymbolGenus.FUNCTION) {
      generateFunctionSingletonField(construct);
      generateFunctionGetInstanceMethod(construct);
    }

    // Generate methods from IR operations
    generateClassMethodsFromIR(construct);

    // Finalize the class
    classWriter.visitEnd();
  }

  /**
   * Store the PROGRAM_ENTRY_POINT_BLOCK instruction to access program parameter metadata.
   */
  private void storeProgramEntryPoint(final IRConstruct construct) {
    // ProgramEntryPointInstr is stored directly on IRConstruct, not nested in operations
    construct.getProgramEntryPoint().ifPresent(entryPoint -> programEntryPoint = entryPoint);
  }

  /**
   * Initialize the actual program class (e.g., introduction/HelloWorld) from the IR construct.
   */
  private void initializeProgramClass(final IRConstruct construct) {
    // Use custom Ek9ClassWriter to avoid ClassLoader dependency during frame computation
    // This allows us to generate multiple classes in same compilation without classpath issues
    classWriter = new Ek9ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    // Get the actual program class name from the IR construct
    final var programClassName = construct.getFullyQualifiedName();
    final var jvmClassName = jvmNameConverter.apply(programClassName);

    classWriter.visit(V21, ACC_PUBLIC, jvmClassName, null, JAVA_LANG_OBJECT, null);

    // Add source file information for debugging - uses normalized .ek9 source filename
    // Normalization strips "./" prefix for jdb compatibility
    final var normalizedSourceFileName = construct.getNormalizedSourceFileName();
    classWriter.visitSource(normalizedSourceFileName, null);

    // Generate and add JSR-45 SMAP for .ek9 source debugging
    final var smapGenerator = new SmapGenerator(getSimpleClassName(programClassName) + ".class");
    smapGenerator.collectFromIRConstruct(construct);
    final var smap = smapGenerator.generate();
    if (smap != null) {
      // Add SourceDebugExtension attribute with SMAP
      classWriter.visitSource(normalizedSourceFileName, smap);
    }
  }

  /**
   * Initialize a general class (not a program) with proper superclass handling.
   * Extracts superclass from symbol hierarchy and generates class declaration.
   * <p>
   * CRITICAL: 'Any' is an interface in Java but a base class in EK9.
   * When superclass is Any: class extends Object implements Any
   * When superclass is not Any: class extends SuperClass (no interfaces)
   * </p>
   */
  private void initializeGeneralClass(final IRConstruct construct) {
    // Use custom Ek9ClassWriter to avoid ClassLoader dependency during frame computation
    // This allows us to generate multiple classes in same compilation without classpath issues
    classWriter = new Ek9ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    // Get the class name from the IR construct
    final var className = construct.getFullyQualifiedName();
    final var jvmClassName = jvmNameConverter.apply(className);

    // Extract superclass from symbol (defaults to Object if no explicit superclass)
    final var symbol = construct.getSymbol();
    final var ek9SuperClassName = determineSuperclassName(symbol);

    // SPECIAL CASE: Any is an interface in Java, not a class
    // If EK9 superclass is Any, generate: class X extends Object implements Any
    // Otherwise: class X extends SuperClass
    final String jvmSuperClassName;
    final String[] interfaces;

    if (EK9_LANG_ANY.equals(ek9SuperClassName)) {
      jvmSuperClassName = JAVA_LANG_OBJECT;
      interfaces = new String[] {EK9_LANG_ANY};
    } else {
      jvmSuperClassName = ek9SuperClassName;
      interfaces = null;  // No interfaces
    }

    // Determine access flags - abstract function traits need ACC_ABSTRACT
    int accessFlags = ACC_PUBLIC;
    if (construct.isFunction() && construct.getSymbol().getGenus() == SymbolGenus.FUNCTION_TRAIT) {
      accessFlags |= ACC_ABSTRACT;
    }

    classWriter.visit(V21, accessFlags, jvmClassName, null, jvmSuperClassName, interfaces);

    // Add source file information for debugging
    final var normalizedSourceFileName = construct.getNormalizedSourceFileName();
    classWriter.visitSource(normalizedSourceFileName, null);

    // Generate and add JSR-45 SMAP for .ek9 source debugging
    final var smapGenerator = new SmapGenerator(getSimpleClassName(className) + ".class");
    smapGenerator.collectFromIRConstruct(construct);
    final var smap = smapGenerator.generate();
    if (smap != null) {
      classWriter.visitSource(normalizedSourceFileName, smap);
    }
  }

  /**
   * Generate field declarations from IR field information.
   * Generates private fields with proper JVM type descriptors.
   */
  private void generateFieldDeclarations(final IRConstruct construct) {
    for (Field field : construct.getFields()) {
      final var fieldName = field.getSymbol().getName();
      final var fieldType = field.getSymbol().getType()
          .orElseThrow(() -> new CompilerException("Field must have a type: " + fieldName));
      final var fieldDescriptor = "L" + jvmNameConverter.apply(fieldType.getFullyQualifiedName()) + ";";

      // Generate field declaration: private fieldType fieldName;
      classWriter.visitField(ACC_PRIVATE, fieldName, fieldDescriptor, null, null).visitEnd();
    }
  }

  /**
   * Generate class methods from IR operations.
   * Handles c_init, i_init, constructors, and general methods/operators.
   */
  private void generateClassMethodsFromIR(final IRConstruct construct) {
    // Process each operation defined in the IR construct
    for (var operation : construct.getOperations()) {
      final var operationName = operation.getSymbol().getName();

      // Map EK9 operations to JVM methods
      switch (operationName) {
        case METHOD_C_INIT -> generateStaticInitializerFromIR(operation);
        case METHOD_I_INIT -> generateInstanceInitFromIR(operation);
        default -> {
          // Check if it's a constructor (method name matches class name)
          final var className = getSimpleClassName(construct.getFullyQualifiedName());
          if (operationName.equals(className)) {
            generateGeneralConstructorFromIR(operation, construct);
          } else {
            // General method or operator
            generateGeneralMethodFromIR(operation);
          }
        }
      }
    }
  }

  /**
   * Generate program methods from IR operations instead of string parsing.
   * Maps EK9 operations to JVM methods.
   */
  private void generateProgramMethodsFromIR(final IRConstruct construct) {
    // Process each operation defined in the IR construct
    for (var operation : construct.getOperations()) {
      final var operationName = operation.getSymbol().getName();

      // Map EK9 operations to JVM methods
      switch (operationName) {
        case METHOD_C_INIT -> generateStaticInitializerFromIR(operation);
        case METHOD_I_INIT -> generateInstanceInitFromIR(operation);
        case METHOD_MAIN -> generateMainMethodFromIR(operation);
        default -> {
          // Check if it's a constructor (method name matches class name)
          final var className = getSimpleClassName(construct.getFullyQualifiedName());
          if (operationName.equals(className)) {
            generateConstructorFromIR(operation);
          } else {
            throw new CompilerException("WARNING: Unhandled operation: " + operationName);
          }
        }
      }
    }
  }

  /**
   * Generate &lt;clinit&gt; static initializer from c_init IR operation.
   * <p>
   * For functions, also initializes the singleton INSTANCE field:
   * INSTANCE = new FunctionType();
   * </p>
   */
  private void generateStaticInitializerFromIR(final OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_STATIC, METHOD_CLINIT, DESC_VOID_TO_VOID, null, null);
    mv.visitCode();

    // For concrete functions (FUNCTION genus): initialize singleton INSTANCE field
    // Abstract function traits (FUNCTION_TRAIT genus) have no INSTANCE to initialize
    final var construct = constructTargetTuple.construct();
    if (construct.getSymbol().getGenus() == SymbolGenus.FUNCTION) {
      generateFunctionSingletonInit(mv, construct);
    }

    // Process the operation's basic block using actual IR instructions
    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      processBasicBlock(mv, basicBlock);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate singleton initialization in &lt;clinit&gt;.
   * <p>
   * Generates: INSTANCE = new FunctionType();
   * Bytecode: NEW, DUP, INVOKESPECIAL &lt;init&gt;, PUTSTATIC INSTANCE
   * </p>
   */
  private void generateFunctionSingletonInit(final MethodVisitor mv, final IRConstruct construct) {
    final var className = construct.getFullyQualifiedName();
    final var jvmClassName = jvmNameConverter.apply(className);
    final var fieldDescriptor = "L" + jvmClassName + ";";

    // NEW FunctionType
    mv.visitTypeInsn(NEW, jvmClassName);

    // DUP (keep reference for PUTSTATIC after constructor returns)
    mv.visitInsn(DUP);

    // INVOKESPECIAL FunctionType.<init>()V
    mv.visitMethodInsn(INVOKESPECIAL, jvmClassName, METHOD_INIT, DESC_VOID_TO_VOID, false);

    // PUTSTATIC INSTANCE
    mv.visitFieldInsn(PUTSTATIC, jvmClassName, FIELD_INSTANCE, fieldDescriptor);
  }

  /**
   * Generate i_init instance method from IR operation.
   */
  private void generateInstanceInitFromIR(final OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_PRIVATE, METHOD_I_INIT, DESC_VOID_TO_VOID, null, null);
    mv.visitCode();

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      processBasicBlock(mv, basicBlock);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate &lt;init&gt; constructor from IR operation.
   */
  private void generateConstructorFromIR(final OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_PUBLIC, METHOD_INIT, DESC_VOID_TO_VOID, null, null);
    mv.visitCode();

    // Call super constructor first
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, METHOD_INIT, DESC_VOID_TO_VOID, false);

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      // Programs typically don't have constructor parameters, but extract them anyway for consistency
      final var parameterNames = extractParameterNames(operation);
      processConstructorBasicBlock(mv, basicBlock, parameterNames);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate constructor for general class with parameter support.
   * The IR already contains super constructor calls - they will be processed by OutputVisitor.
   */
  private void generateGeneralConstructorFromIR(final OperationInstr operation, final IRConstruct construct) {
    // Build constructor descriptor from operation signature (e.g., "(Lorg/ek9/lang/String;Lorg/ek9/lang/Integer;)V")
    final var constructorDescriptor = buildConstructorDescriptor(operation);

    final var mv = classWriter.visitMethod(ACC_PUBLIC, METHOD_INIT, constructorDescriptor, null, null);
    mv.visitCode();

    // CRITICAL: Inject super() call only if IR doesn't have one.
    // The IR generator adds super() calls when there's an explicit super type.
    // We only inject when:
    // 1. No this() delegation (delegated constructor handles super)
    // 2. IR doesn't have super call (no explicit super type)
    if (!hasThisDelegation(operation) && needsObjectSuperCall(construct)) {
      injectSuperConstructorCall(mv, construct);
    }

    // IR already contains super constructor call for non-Any superclasses - OutputVisitor will handle it
    // Process the constructor body which includes:
    // 1. Super constructor call (from IR, if superclass is not Any)
    // 2. Field assignments
    // 3. Any other initialization logic
    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      // Extract parameter names from operation symbol
      final var parameterNames = extractParameterNames(operation);
      processConstructorBasicBlock(mv, basicBlock, parameterNames);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate general method (not c_init, i_init, or constructor).
   * Handles methods with parameters and return types.
   * Maps EK9 operators to JVM method names using OperatorMap.
   * <p>
   * For abstract function traits (FUNCTION_TRAIT genus), the _call method is abstract:
   * - No method body generated
   * - ACC_ABSTRACT flag added to method access flags
   * </p>
   */
  private void generateGeneralMethodFromIR(final OperationInstr operation) {
    final var symbol = operation.getSymbol();
    final var symbolName = symbol.getName();

    // Map EK9 operators to JVM method names
    final String methodName;
    if (symbol instanceof MethodSymbol methodSymbol && methodSymbol.isOperator()) {
      methodName = operatorMap.getForward(symbolName);
    } else {
      methodName = symbolName;
    }

    final var methodDescriptor = buildGeneralMethodDescriptor(operation);

    // Check if this is an abstract method in a FUNCTION_TRAIT
    final var construct = constructTargetTuple.construct();
    final boolean isAbstractMethod = construct.isFunction()
        && construct.getSymbol().getGenus() == SymbolGenus.FUNCTION_TRAIT
        && "_call".equals(methodName);

    // Determine access flags - abstract methods need ACC_ABSTRACT
    int accessFlags = ACC_PUBLIC;
    if (isAbstractMethod) {
      accessFlags |= ACC_ABSTRACT;
    }

    final var mv = classWriter.visitMethod(accessFlags, methodName, methodDescriptor, null, null);

    // Abstract methods have no body - just signature
    if (isAbstractMethod) {
      mv.visitEnd();
      return;
    }

    mv.visitCode();

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      // Pre-register method parameters so they're at correct local variable slots
      final var parameterNames = extractParameterNames(operation);
      processBasicBlock(mv, basicBlock, parameterNames, false);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Build JVM constructor descriptor from operation signature.
   * Example: (String, Integer) -> "(Lorg/ek9/lang/String;Lorg/ek9/lang/Integer;)V"
   */
  private String buildConstructorDescriptor(final OperationInstr operation) {
    // Extract parameters from operation symbol (MethodSymbol for constructors)
    final var symbol = operation.getSymbol();

    // Get INPUT parameters - filter out returning parameters (declared with <-)
    // Returning parameters are return values, not JVM constructor parameters
    final List<ISymbol> parameters;
    if (symbol instanceof MethodSymbol methodSymbol) {
      parameters = methodSymbol.getCallParameters().stream()
          .filter(param -> !param.isReturningParameter())
          .toList();
    } else {
      parameters = Collections.emptyList();
    }

    if (parameters.isEmpty()) {
      return DESC_VOID_TO_VOID;  // No-arg constructor
    }

    final var descriptor = new StringBuilder("(");
    for (var param : parameters) {
      final var paramType = param.getType()
          .orElseThrow(() -> new CompilerException("Parameter must have type: " + param.getName()));
      descriptor.append(descriptorConverter.apply(paramType.getFullyQualifiedName()));
    }
    descriptor.append(")V");  // Constructors return void

    return descriptor.toString();
  }

  /**
   * Build JVM method descriptor from operation signature.
   * Example: code()->Integer becomes "()Lorg/ek9/lang/Integer;"
   */
  private String buildGeneralMethodDescriptor(final OperationInstr operation) {
    final var returnType = operation.getSymbol().getType()
        .orElseThrow(() -> new CompilerException("Method must have return type: " + operation.getSymbol().getName()));

    // Get INPUT parameters from the symbol's call parameters (exclude returning parameters)
    // Returning parameters (declared with <-) are return values, not JVM method parameters
    final var symbol = operation.getSymbol();
    final List<ISymbol> parameters;
    if (symbol instanceof MethodSymbol methodSymbol) {
      parameters = methodSymbol.getCallParameters().stream()
          .filter(param -> !param.isReturningParameter())
          .toList();
    } else {
      parameters = Collections.emptyList();
    }

    final var descriptor = new StringBuilder("(");
    for (var param : parameters) {
      final var paramType = param.getType()
          .orElseThrow(() -> new CompilerException("Parameter must have type: " + param.getName()));
      descriptor.append(descriptorConverter.apply(paramType.getFullyQualifiedName()));
    }
    descriptor.append(")");

    // Add return type descriptor
    descriptor.append(descriptorConverter.apply(returnType.getFullyQualifiedName()));

    return descriptor.toString();
  }

  /**
   * Generate _main instance method from IR operation.
   * Uses the actual parameter signature from the OperationInstr.
   * Pre-registers method parameters in variable map to prevent null overwriting.
   */
  private void generateMainMethodFromIR(final OperationInstr operation) {
    // Build method descriptor from operation signature
    final var methodDescriptor = buildMethodDescriptor(operation);

    final var mv = classWriter.visitMethod(ACC_PUBLIC, METHOD_MAIN, methodDescriptor, null, null);
    mv.visitCode();

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      // Pre-register method parameters before processing instructions
      final var parameterDetails = getParameterDetailsForMethod();
      processBasicBlockWithParameters(mv, basicBlock, parameterDetails);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Get parameter details for the current method from PROGRAM_ENTRY_POINT_BLOCK.
   * Returns list of parameter details (name + type) in order.
   */
  private List<ParameterDetails> getParameterDetailsForMethod() {
    if (programEntryPoint != null) {
      final var programClassName = getProgramClassName();

      // Find matching program in entry point
      for (var programDetails : programEntryPoint.getAvailablePrograms()) {
        if (programDetails.qualifiedName().equals(programClassName)) {
          return programDetails.parameterSignature();
        }
      }
    }

    return Collections.emptyList();
  }

  /**
   * Build JVM method descriptor from program's parameter signature in PROGRAM_ENTRY_POINT_BLOCK.
   * Example: (org.ek9.lang::String) -> "(Lorg/ek9/lang/String;)V"
   */
  private String buildMethodDescriptor(final OperationInstr operation) {
    // For _main method, get parameter signature from PROGRAM_ENTRY_POINT_BLOCK
    if (programEntryPoint != null) {
      final var programClassName = getProgramClassName();

      // Find matching program in entry point
      for (var programDetails : programEntryPoint.getAvailablePrograms()) {
        if (programDetails.qualifiedName().equals(programClassName)) {
          // Build descriptor from parameter signature
          final var descriptor = new StringBuilder("(");

          for (var param : programDetails.parameterSignature()) {
            descriptor.append(descriptorConverter.apply(param.type()));
          }

          descriptor.append(")V"); // _main returns void
          return descriptor.toString();
        }
      }
    }

    // Fallback for methods without parameters
    return DESC_VOID_TO_VOID;
  }

  /**
   * Get the fully qualified program class name being generated.
   */
  private String getProgramClassName() {
    return constructTargetTuple.construct().getFullyQualifiedName();
  }

  /**
   * Populate field metadata map from IR construct fields.
   * Called once per method during MethodContext initialization.
   * Maps "this.fieldName" to JVM field descriptor for putfield/getfield instructions.
   */
  private void populateFieldMetadata(final IRConstruct construct,
                                     final AbstractAsmGenerator.MethodContext methodContext) {
    for (Field field : construct.getFields()) {
      final var fieldName = field.getSymbol().getName();
      final var fieldType = field.getSymbol().getType()
          .orElseThrow(() -> new CompilerException("Field must have type: " + fieldName));
      final var fieldDescriptor = "L" + jvmNameConverter.apply(fieldType.getFullyQualifiedName()) + ";";

      methodContext.fieldDescriptorMap.put("this." + fieldName, fieldDescriptor);
    }
  }

  /**
   * Process a basic block with parameter details for _main method context.
   * Pre-registers parameters in variable map and LocalVariableTable metadata.
   */
  private void processBasicBlockWithParameters(final MethodVisitor mv,
                                               final BasicBlockInstr basicBlock,
                                               final List<ParameterDetails> parameterDetails) {
    // Cast visitor to OutputVisitor to access generator setup methods
    final OutputVisitor outputVisitor = (OutputVisitor) visitor;

    // Create fresh MethodContext for this method
    final var methodContext = new AbstractAsmGenerator.MethodContext();

    // Populate field metadata for field access
    populateFieldMetadata(constructTargetTuple.construct(), methodContext);

    // Pre-register method parameters (prevents null overwriting)
    // Also register for LocalVariableTable (enables jdb to show parameter names/values)
    // Instance method parameters start at slot 1 (slot 0 is 'this')
    int parameterSlot = 1;
    for (ParameterDetails param : parameterDetails) {
      final var paramName = param.name();
      methodContext.variableMap.put(paramName, parameterSlot);

      // Create LocalVariableInfo for parameter (scopeId=null, will use method-wide scope)
      final var paramTypeDescriptor = jvmNameConverter.apply(param.type());
      final var varInfo = new AbstractAsmGenerator.LocalVariableInfo(
          paramName,
          "L" + paramTypeDescriptor + ";",
          null  // scopeId is null for parameters - will use method-wide _call scope
      );
      varInfo.slot = parameterSlot;  // Set slot immediately
      methodContext.localVariableMetadata.put(paramName, varInfo);

      parameterSlot++;
    }
    // Update nextVariableSlot to account for pre-registered parameters
    methodContext.nextVariableSlot = parameterSlot;

    // Share method context and method visitor with all generators
    outputVisitor.setMethodContext(methodContext, mv, false);  // false = not a constructor

    // Process each IR instruction using visitor pattern
    for (var instruction : basicBlock.getInstructions()) {
      instruction.accept(visitor);
    }

    // Generate LocalVariableTable after processing all instructions
    outputVisitor.generateLocalVariableTable();
  }

  /**
   * Process a basic block for constructor context.
   */
  private void processConstructorBasicBlock(final MethodVisitor mv,
                                            final BasicBlockInstr basicBlock,
                                            final List<String> parameterNames) {
    processBasicBlock(mv, basicBlock, parameterNames, true);
  }

  /**
   * Process a basic block using visitor pattern.
   * Default: no parameters, not a constructor.
   */
  private void processBasicBlock(final MethodVisitor mv,
                                 final BasicBlockInstr basicBlock) {
    processBasicBlock(mv, basicBlock, Collections.emptyList(), false);
  }

  /**
   * Process a basic block using visitor pattern delegation.
   * Sets up method context, then delegates to OutputVisitor for each instruction.
   */
  private void processBasicBlock(final MethodVisitor mv,
                                 final BasicBlockInstr basicBlock,
                                 final List<String> parameterNames,
                                 final boolean isConstructor) {
    // Cast visitor to OutputVisitor to access generator setup methods
    final OutputVisitor outputVisitor = (OutputVisitor) visitor;

    // Create fresh MethodContext for this method
    final var methodContext = new AbstractAsmGenerator.MethodContext();

    // Populate field metadata for field access
    populateFieldMetadata(constructTargetTuple.construct(), methodContext);

    // Pre-register method parameters (prevents null overwriting)
    // Instance method parameters start at slot 1 (slot 0 is 'this')
    int parameterSlot = 1;
    for (String paramName : parameterNames) {
      methodContext.variableMap.put(paramName, parameterSlot++);
    }
    // Update nextVariableSlot to account for pre-registered parameters
    methodContext.nextVariableSlot = parameterSlot;

    // Share method context and method visitor with all generators
    outputVisitor.setMethodContext(methodContext, mv, isConstructor);

    // Process each IR instruction using visitor pattern
    for (var instruction : basicBlock.getInstructions()) {
      instruction.accept(visitor);
    }

    // Generate LocalVariableTable after processing all instructions
    outputVisitor.generateLocalVariableTable();
  }


  /**
   * Extract simple class name from fully qualified name.
   */
  private String getSimpleClassName(final String fullyQualifiedName) {
    final var lastIndex = fullyQualifiedName.lastIndexOf("::");
    return lastIndex >= 0 ? fullyQualifiedName.substring(lastIndex + 2) : fullyQualifiedName;
  }

  /**
   * Extract INPUT parameter names from operation symbol for method/constructor.
   * Method parameters must be pre-registered in the variable map to prevent
   * the IR from allocating temporary variables in their slots.
   * <p>
   * IMPORTANT: Filters out returning parameters (declared with &lt;-) since those
   * are return values, not input parameters. Only input parameters (declared with -&gt;)
   * occupy JVM method parameter slots.
   * </p>
   *
   * @param operation The operation (method/constructor) to extract parameters from
   * @return List of INPUT parameter names in declaration order
   */
  private List<String> extractParameterNames(final OperationInstr operation) {
    final var symbol = operation.getSymbol();
    if (symbol instanceof MethodSymbol methodSymbol) {
      return methodSymbol.getCallParameters().stream()
          .filter(param -> !param.isReturningParameter())  // Exclude return values
          .map(ISymbol::getName)
          .toList();
    }
    return Collections.emptyList();
  }

  /**
   * Determine the JVM superclass name for a symbol.
   * <p>
   * Handles both aggregate symbols (classes) and function symbols.
   * </p>
   * <p>
   * IMPORTANT: Functions always extend Object directly in bytecode.
   * The abstract function types (like _Routine, _BiRoutine) are for EK9 type checking,
   * not JVM inheritance. Abstract function types don't have bytecode generated yet.
   * </p>
   * <p>
   * TODO: IMPLICIT TYPE BYTECODE GENERATION REQUIRED
   * When implementing generics (List&lt;String&gt;, Iterator&lt;T&gt;) and true function polymorphism
   * (holding concrete function via abstract signature type), we must generate bytecode for
   * implicit types like _Routine_*, _List_*, _Iterator_*. Currently these types only exist
   * in the EK9 type system for compile-time checking. This workaround (extending Object)
   * supports direct inheritance chains (helloGreeting → baseGreeting → Object) but NOT
   * polymorphic usage via abstract function signature types.
   * See also: injectSuperConstructorCall(), needsObjectSuperCall()
   * </p>
   */
  private String determineSuperclassName(final ISymbol symbol) {
    // Handle function symbols - may extend other functions or implicit types
    if (symbol instanceof FunctionSymbol functionSymbol) {
      final var superFunction = functionSymbol.getSuperFunction();
      if (superFunction.isPresent()) {
        final var superName = superFunction.get().getFullyQualifiedName();
        // TODO: Generate bytecode for implicit types (org.ek9.lang::_*) when implementing
        // generics and function polymorphism. Currently we extend Object as a workaround.
        if (superName.startsWith("org.ek9.lang::_")) {
          return JAVA_LANG_OBJECT;
        }
        // User-defined super function - use it as superclass
        return jvmNameConverter.apply(superName);
      }
      // No super function - extend Object directly
      return JAVA_LANG_OBJECT;
    }

    // Handle aggregate symbols - extend declared superclass
    if (symbol instanceof IAggregateSymbol aggregateSymbol) {
      return aggregateSymbol.getSuperAggregate()
          .map(ISymbol::getFullyQualifiedName)
          .map(jvmNameConverter)
          .orElse(JAVA_LANG_OBJECT);
    }

    // Default to Object
    return JAVA_LANG_OBJECT;
  }

  /**
   * Inject appropriate super constructor call for the construct.
   * <p>
   * Determines the correct superclass to call based on construct type:
   * - Functions with super function: call super function's constructor
   * - Functions without super function: call Object's constructor
   * - Classes extending Any: call Object's constructor (Any is interface in Java)
   * - Classes with explicit superclass: call that superclass's constructor
   * - Classes without superclass: call Object's constructor
   * </p>
   */
  private void injectSuperConstructorCall(final MethodVisitor mv, final IRConstruct construct) {
    final var symbol = construct.getSymbol();

    // Functions may extend other functions, so check for super function
    if (symbol instanceof FunctionSymbol functionSymbol) {
      mv.visitVarInsn(ALOAD, 0);
      final var superFunction = functionSymbol.getSuperFunction();
      if (superFunction.isEmpty()) {
        // No super function - call Object constructor directly
        mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, METHOD_INIT, DESC_VOID_TO_VOID, false);
      } else {
        final var superName = superFunction.get().getFullyQualifiedName();
        // TODO: When bytecode generation for implicit types (org.ek9.lang::_*) is implemented,
        // call the actual super constructor instead of Object. See determineSuperclassName().
        if (superName.startsWith("org.ek9.lang::_")) {
          mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, METHOD_INIT, DESC_VOID_TO_VOID, false);
        } else {
          // User-defined super function - call its constructor
          final var superJvmName = jvmNameConverter.apply(superName);
          mv.visitMethodInsn(INVOKESPECIAL, superJvmName, METHOD_INIT, DESC_VOID_TO_VOID, false);
        }
      }
      return;
    }

    // Handle aggregate symbols (classes, records, etc.)
    if (symbol instanceof IAggregateSymbol aggregateSymbol) {
      final var superAggregate = aggregateSymbol.getSuperAggregate();
      mv.visitVarInsn(ALOAD, 0);
      if (superAggregate.isEmpty() || "org.ek9.lang::Any".equals(superAggregate.get().getFullyQualifiedName())) {
        // No superclass or extends Any (interface) - call Object constructor
        mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, METHOD_INIT, DESC_VOID_TO_VOID, false);
      } else {
        // Has explicit superclass - call that constructor
        final var superJvmName = jvmNameConverter.apply(superAggregate.get().getFullyQualifiedName());
        mv.visitMethodInsn(INVOKESPECIAL, superJvmName, METHOD_INIT, DESC_VOID_TO_VOID, false);
      }
      return;
    }

    // Fallback: call Object constructor
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_OBJECT, METHOD_INIT, DESC_VOID_TO_VOID, false);
  }

  /**
   * Check if construct needs an explicit Object.&lt;init&gt;() super call injected.
   * <p>
   * Returns true when the IR does NOT contain a super() call but bytecode requires one:
   * 1. Classes extending Any (Any is an interface in Java, super() goes to Object)
   * 2. Classes/functions with no explicit superclass (implicitly extend Object)
   * </p>
   *
   * @param construct The IR construct to check
   * @return true if Object super() call needs to be injected
   */
  private boolean needsObjectSuperCall(final IRConstruct construct) {
    final var symbol = construct.getSymbol();

    // Handle aggregate symbols (classes, records, etc.)
    if (symbol instanceof IAggregateSymbol aggregateSymbol) {
      final var superAggregate = aggregateSymbol.getSuperAggregate();

      // No superclass = defaults to Object, need to inject super()
      if (superAggregate.isEmpty()) {
        return true;
      }

      // Extends Any = Java Object (since Any is interface), need to inject super()
      final var superName = superAggregate.get().getFullyQualifiedName();
      return "org.ek9.lang::Any".equals(superName);
    }

    // Handle function symbols (functions use getSuperFunction instead of getSuperAggregate)
    if (symbol instanceof FunctionSymbol functionSymbol) {
      final var superFunction = functionSymbol.getSuperFunction();
      // No super function = defaults to Object, need to inject super()
      if (superFunction.isEmpty()) {
        return true;
      }
      // TODO: When bytecode generation for implicit types is implemented, this logic
      // will need updating. See determineSuperclassName() for the main TODO.
      // Currently: implicit types (org.ek9.lang::_*) don't have bytecode, so we inject Object super call.
      final var superName = superFunction.get().getFullyQualifiedName();
      return superName.startsWith("org.ek9.lang::_");
    }

    return false;
  }

  /**
   * Check if construct extends Any (EK9's universal base type).
   * <p>
   * Any is an interface in Java (to enable compatibility with all types including primitives),
   * but conceptually it's EK9's base class. Classes extending Any actually extend Object
   * and implement Any interface.
   * </p>
   * <p>
   * This is a JVM-specific quirk - the IR correctly treats Any as the base type,
   * but bytecode generation must inject Object.&lt;init&gt;() calls.
   * </p>
   *
   * @param construct The IR construct to check
   * @return true if construct directly extends Any
   */
  private boolean extendsAny(final IRConstruct construct) {
    final var symbol = construct.getSymbol();
    if (symbol instanceof IAggregateSymbol aggregateSymbol) {
      return aggregateSymbol.getSuperAggregate()
          .map(superSymbol -> "org.ek9.lang::Any".equals(superSymbol.getFullyQualifiedName()))
          .orElse(false);
    }
    return false;
  }

  /**
   * Check if constructor delegates to another constructor via this().
   * Detects this() calls by looking for CALL instructions with target="this" and method="&lt;init&gt;".
   *
   * @param operation The constructor operation IR
   * @return true if constructor contains this() delegation
   */
  private boolean hasThisDelegation(final OperationInstr operation) {
    final var basicBlock = operation.getBody();
    if (basicBlock == null) {
      return false;
    }

    // Check all instructions in the constructor
    for (final var instr : basicBlock.getInstructions()) {
      if (instr instanceof CallInstr callInstr && "this".equals(callInstr.getTargetObject())
          && METHOD_INIT.equals(callInstr.getMethodName())) {
        return true;
      }
    }

    return false;
  }

  /**
   * Generate static INSTANCE field for function singleton pattern.
   * <p>
   * Functions in EK9 are singleton objects - each named function has exactly one instance.
   * This generates: private static final FunctionType INSTANCE;
   * The field is initialized in &lt;clinit&gt; via generateStaticInitializerFromIR.
   * </p>
   *
   * @param construct The function IR construct
   */
  private void generateFunctionSingletonField(final IRConstruct construct) {
    final var className = construct.getFullyQualifiedName();
    final var jvmClassName = jvmNameConverter.apply(className);
    final var fieldDescriptor = "L" + jvmClassName + ";";

    // Generate: private static final FunctionType INSTANCE;
    classWriter.visitField(
        ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
        FIELD_INSTANCE,
        fieldDescriptor,
        null,
        null
    ).visitEnd();
  }

  /**
   * Generate static getInstance() method for function singleton pattern.
   * <p>
   * This generates:
   * public static FunctionType getInstance() { return INSTANCE; }
   * </p>
   *
   * @param construct The function IR construct
   */
  private void generateFunctionGetInstanceMethod(final IRConstruct construct) {
    final var className = construct.getFullyQualifiedName();
    final var jvmClassName = jvmNameConverter.apply(className);
    final var returnDescriptor = "L" + jvmClassName + ";";
    final var methodDescriptor = "()" + returnDescriptor;

    final var mv = classWriter.visitMethod(
        ACC_PUBLIC | ACC_STATIC,
        METHOD_GET_INSTANCE,
        methodDescriptor,
        null,
        null
    );
    mv.visitCode();

    // GETSTATIC this.INSTANCE
    mv.visitFieldInsn(GETSTATIC, jvmClassName, FIELD_INSTANCE, returnDescriptor);

    // ARETURN
    mv.visitInsn(ARETURN);

    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  byte[] getByteCode() {
    if (classWriter != null) {
      return classWriter.toByteArray();
    }
    return new byte[0];
  }

  /**
   * Get the ClassWriter for use by specialized ASM generators.
   * This allows generators to share the same ClassWriter instance.
   */
  ClassWriter getClassWriter() {
    if (classWriter == null) {
      // Initialize if not already done
      classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    }
    return classWriter;
  }
}
