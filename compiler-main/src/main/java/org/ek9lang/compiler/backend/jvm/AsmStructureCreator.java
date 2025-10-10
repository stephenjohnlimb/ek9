package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.JVMTypeNames.DESC_VOID_TO_VOID;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_OBJECT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_CLINIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_C_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_I_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_MAIN;

import java.util.Collections;
import java.util.List;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.data.ParameterDetails;
import org.ek9lang.compiler.ir.instructions.BasicBlockInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.ek9lang.core.CompilerException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Designed to capture the ASM specifics for byte code generation.
 * Generates the actual program class from IR operations (not ek9.Main).
 */
public final class AsmStructureCreator implements Opcodes {

  private final ConstructTargetTuple constructTargetTuple;
  private final INodeVisitor visitor;
  private final FullyQualifiedJvmName jvmNameConverter = new FullyQualifiedJvmName();
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
    throw new CompilerException("Constructs other than program not yet supported");
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
    // Enable COMPUTE_FRAMES to automatically generate stack map frames required for JVM verification
    classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

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
   */
  private void generateStaticInitializerFromIR(final OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_STATIC, METHOD_CLINIT, DESC_VOID_TO_VOID, null, null);
    mv.visitCode();

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
      processConstructorBasicBlock(mv, basicBlock); // true = isConstructor
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
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
          final var typeConverter = new EK9TypeToJVMDescriptor();

          for (var param : programDetails.parameterSignature()) {
            descriptor.append(typeConverter.apply(param.type()));
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
                                            final BasicBlockInstr basicBlock) {
    processBasicBlock(mv, basicBlock, Collections.emptyList(), true);
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
