package org.ek9lang.compiler.backend.jvm;

import java.util.HashMap;
import java.util.Map;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
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
  private ClassWriter classWriter;
  private final Map<String, String> programMethodNames = new HashMap<>();

  // Track if we've already processed the program entry point
  private boolean programEntryPointProcessed = false;
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
    // Disable automatic frame computation to avoid stack verification issues during development
    classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

    // Get the actual program class name from the IR construct
    final var programClassName = construct.getFullyQualifiedName();
    final var jvmClassName = programClassName.replace(".", "/").replace("::", "/");

    classWriter.visit(V21, ACC_PUBLIC, jvmClassName, null, "java/lang/Object", null);

    // Add source file information for debugging
    final var simpleClassName = programClassName.substring(programClassName.lastIndexOf("::") + 2);
    classWriter.visitSource(simpleClassName + ".java", null);
  }

  /**
   * Generate program methods from IR operations instead of string parsing.
   * Maps EK9 operations to JVM methods:
   * - c_init -> <clinit> (static initializer)
   * - i_init -> i_init (instance method)
   * - constructor -> <init> (constructor)
   * - _main -> _main (instance method)
   */
  private void generateProgramMethodsFromIR(final IRConstruct construct) {
    // Process each operation defined in the IR construct
    for (var operation : construct.getOperations()) {
      final var operationName = operation.getSymbol().getName();

      // Map EK9 operations to JVM methods
      switch (operationName) {
        case "c_init" -> generateStaticInitializerFromIR(operation);
        case "i_init" -> generateInstanceInitFromIR(operation);
        case "_main" -> generateMainMethodFromIR(operation);
        default -> {
          // Check if it's a constructor (method name matches class name)
          final var className = getSimpleClassName(construct.getFullyQualifiedName());
          if (operationName.equals(className)) {
            generateConstructorFromIR(operation);
          } else {
            System.err.println("WARNING: Unhandled operation: " + operationName);
          }
        }
      }
    }
  }

  /**
   * Generate <clinit> static initializer from c_init IR operation.
   */
  private void generateStaticInitializerFromIR(final org.ek9lang.compiler.ir.instructions.OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
    mv.visitCode();

    // Process the operation's basic block using actual IR instructions
    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      processBasicBlockWithTypedInstructions(mv, basicBlock);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate i_init instance method from IR operation.
   */
  private void generateInstanceInitFromIR(final org.ek9lang.compiler.ir.instructions.OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_PRIVATE, "i_init", "()V", null, null);
    mv.visitCode();

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      processBasicBlockWithTypedInstructions(mv, basicBlock);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate <init> constructor from IR operation.
   */
  private void generateConstructorFromIR(final org.ek9lang.compiler.ir.instructions.OperationInstr operation) {
    final var mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();

    // Call super constructor first
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      processBasicBlockWithTypedInstructions(mv, basicBlock, true); // true = isConstructor
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
  private void generateMainMethodFromIR(final org.ek9lang.compiler.ir.instructions.OperationInstr operation) {
    // Build method descriptor from operation signature
    final var methodDescriptor = buildMethodDescriptor(operation);

    final var mv = classWriter.visitMethod(ACC_PUBLIC, "_main", methodDescriptor, null, null);
    mv.visitCode();

    final var basicBlock = operation.getBody();
    if (basicBlock != null) {
      // Pre-register method parameters before processing instructions
      final var parameterNames = getParameterNamesForMethod();
      processBasicBlockWithTypedInstructions(mv, basicBlock, parameterNames);
    }

    // Don't add extra RETURN - IR instructions already include RETURN via BranchInstr
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Get parameter names for the current method from PROGRAM_ENTRY_POINT_BLOCK.
   * Returns list of parameter names in order (e.g., ["message"] for HelloString).
   */
  private java.util.List<String> getParameterNamesForMethod() {
    if (programEntryPoint != null) {
      final var programClassName = getProgramClassName();

      // Find matching program in entry point
      for (var programDetails : programEntryPoint.getAvailablePrograms()) {
        if (programDetails.qualifiedName().equals(programClassName)) {
          // Extract parameter names from parameter signature
          return programDetails.parameterSignature().stream()
              .map(org.ek9lang.compiler.ir.data.ParameterDetails::name)
              .collect(java.util.stream.Collectors.toList());
        }
      }
    }

    return java.util.Collections.emptyList();
  }

  /**
   * Build JVM method descriptor from program's parameter signature in PROGRAM_ENTRY_POINT_BLOCK.
   * Example: (org.ek9.lang::String) -> "(Lorg/ek9/lang/String;)V"
   */
  private String buildMethodDescriptor(final org.ek9lang.compiler.ir.instructions.OperationInstr operation) {
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
    return "()V";
  }

  /**
   * Get the fully qualified program class name being generated.
   */
  private String getProgramClassName() {
    return constructTargetTuple.construct().getFullyQualifiedName();
  }

  /**
   * Process a basic block using typed IR instructions via visitor pattern.
   * Uses specialized ASM generators for each instruction type.
   */
  private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                     final org.ek9lang.compiler.ir.instructions.BasicBlockInstr basicBlock) {
    // Create temporary visitor to process instructions with access to current MethodVisitor
    final var instructionVisitor = new InstructionVisitor(mv);

    // Process each typed IR instruction using visitor pattern
    for (var instruction : basicBlock.getInstructions()) {
      // Use visitor pattern to dispatch to appropriate specialized generator
      if (instruction instanceof org.ek9lang.compiler.ir.instructions.CallInstr callInstr) {
        instructionVisitor.processCallInstruction(callInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.LiteralInstr literalInstr) {
        instructionVisitor.processLiteralInstruction(literalInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.MemoryInstr memoryInstr) {
        instructionVisitor.processMemoryInstruction(memoryInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.ScopeInstr scopeInstr) {
        instructionVisitor.processScopeInstruction(scopeInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.BranchInstr branchInstr) {
        instructionVisitor.processBranchInstruction(branchInstr);
      } else {
        System.err.println("WARNING: Unhandled typed IR instruction: " + instruction.getClass().getSimpleName());
      }
    }
  }

  /**
   * Overloaded version with parameter names for _main method context.
   * Pre-registers parameters in variable map to prevent null overwriting.
   */
  private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                     final org.ek9lang.compiler.ir.instructions.BasicBlockInstr basicBlock,
                                                     final java.util.List<String> parameterNames) {
    // Create temporary visitor with pre-registered parameters
    final var instructionVisitor = new InstructionVisitor(mv, parameterNames);

    // Process each typed IR instruction using visitor pattern
    for (var instruction : basicBlock.getInstructions()) {
      // Use visitor pattern to dispatch to appropriate specialized generator
      if (instruction instanceof org.ek9lang.compiler.ir.instructions.CallInstr callInstr) {
        instructionVisitor.processCallInstruction(callInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.LiteralInstr literalInstr) {
        instructionVisitor.processLiteralInstruction(literalInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.MemoryInstr memoryInstr) {
        instructionVisitor.processMemoryInstruction(memoryInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.ScopeInstr scopeInstr) {
        instructionVisitor.processScopeInstruction(scopeInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.BranchInstr branchInstr) {
        instructionVisitor.processBranchInstruction(branchInstr);
      } else {
        System.err.println("WARNING: Unhandled typed IR instruction: " + instruction.getClass().getSimpleName());
      }
    }
  }

  /**
   * Overloaded version for constructor context.
   */
  private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                     final org.ek9lang.compiler.ir.instructions.BasicBlockInstr basicBlock,
                                                     final boolean isConstructor) {
    // Create temporary visitor to process instructions with access to current MethodVisitor and constructor flag
    final var instructionVisitor = new InstructionVisitor(mv, isConstructor);

    // Process each typed IR instruction using visitor pattern
    for (var instruction : basicBlock.getInstructions()) {
      // Use visitor pattern to dispatch to appropriate specialized generator
      if (instruction instanceof org.ek9lang.compiler.ir.instructions.CallInstr callInstr) {
        instructionVisitor.processCallInstruction(callInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.LiteralInstr literalInstr) {
        instructionVisitor.processLiteralInstruction(literalInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.MemoryInstr memoryInstr) {
        instructionVisitor.processMemoryInstruction(memoryInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.ScopeInstr scopeInstr) {
        instructionVisitor.processScopeInstruction(scopeInstr);
      } else if (instruction instanceof org.ek9lang.compiler.ir.instructions.BranchInstr branchInstr) {
        instructionVisitor.processBranchInstruction(branchInstr);
      } else {
        System.err.println("WARNING: Unhandled typed IR instruction: " + instruction.getClass().getSimpleName());
      }
    }
  }

  /**
   * Inner class to handle IR instruction processing with access to current MethodVisitor.
   * Uses existing specialized ASM generators instead of duplicating functionality.
   */
  private class InstructionVisitor {
    private final MethodVisitor methodVisitor;
    private final CallInstrAsmGenerator callInstrGenerator;
    private final LiteralInstrAsmGenerator literalInstrGenerator;
    private final MemoryInstrAsmGenerator memoryInstrGenerator;
    private final ScopeInstrAsmGenerator scopeInstrGenerator;
    private final boolean isConstructor;

    InstructionVisitor(final MethodVisitor mv) {
      this(mv, false);
    }

    InstructionVisitor(final MethodVisitor mv, final boolean isConstructor) {
      this(mv, isConstructor, java.util.Collections.emptyList());
    }

    /**
     * Constructor with parameter names for pre-registration in variable map.
     * Parameters are allocated to local variable slots 1, 2, 3, ... (slot 0 is 'this').
     */
    InstructionVisitor(final MethodVisitor mv, final java.util.List<String> parameterNames) {
      this(mv, false, parameterNames);
    }

    private InstructionVisitor(final MethodVisitor mv, final boolean isConstructor,
                               final java.util.List<String> parameterNames) {
      this.methodVisitor = mv;
      this.isConstructor = isConstructor;

      // Cast visitor to OutputVisitor since that's what created this AsmStructureCreator
      final OutputVisitor outputVisitor = (OutputVisitor) visitor;

      // Create instances of existing specialized generators with proper OutputVisitor
      this.callInstrGenerator = new CallInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);
      this.literalInstrGenerator = new LiteralInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);
      this.memoryInstrGenerator = new MemoryInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);
      this.scopeInstrGenerator = new ScopeInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);

      // Create shared method context for this method's variable slot allocation
      final AbstractAsmGenerator.MethodContext sharedContext = new AbstractAsmGenerator.MethodContext();

      // Pre-register method parameters in variable map (prevents null overwriting)
      // Instance method parameters start at slot 1 (slot 0 is 'this')
      int parameterSlot = 1;
      for (String paramName : parameterNames) {
        sharedContext.variableMap.put(paramName, parameterSlot++);
      }
      // Update nextVariableSlot to account for pre-registered parameters
      sharedContext.nextVariableSlot = parameterSlot;

      // Set shared context for all generators to ensure coordinated variable slot allocation
      this.callInstrGenerator.setSharedMethodContext(sharedContext);
      this.literalInstrGenerator.setSharedMethodContext(sharedContext);
      this.memoryInstrGenerator.setSharedMethodContext(sharedContext);
      this.scopeInstrGenerator.setSharedMethodContext(sharedContext);

      // Set the current method visitor for all generators
      this.callInstrGenerator.setCurrentMethodVisitor(mv);
      this.literalInstrGenerator.setCurrentMethodVisitor(mv);
      this.memoryInstrGenerator.setCurrentMethodVisitor(mv);
      this.scopeInstrGenerator.setCurrentMethodVisitor(mv);
    }

    void processCallInstruction(final org.ek9lang.compiler.ir.instructions.CallInstr callInstr) {
      // Delegate to existing specialized CallInstrAsmGenerator
      callInstrGenerator.generateCall(callInstr);
    }

    void processLiteralInstruction(final org.ek9lang.compiler.ir.instructions.LiteralInstr literalInstr) {
      // Delegate to existing specialized LiteralInstrAsmGenerator
      literalInstrGenerator.generateLiteral(literalInstr);
    }

    void processMemoryInstruction(final org.ek9lang.compiler.ir.instructions.MemoryInstr memoryInstr) {
      // Delegate to existing specialized MemoryInstrAsmGenerator
      memoryInstrGenerator.generateMemoryOperation(memoryInstr);
    }

    void processScopeInstruction(final org.ek9lang.compiler.ir.instructions.ScopeInstr scopeInstr) {
      // Delegate to existing specialized ScopeInstrAsmGenerator
      scopeInstrGenerator.generateScopeOperation(scopeInstr);
    }

    void processBranchInstruction(final org.ek9lang.compiler.ir.instructions.BranchInstr branchInstr) {
      // BranchInstr represents RETURN in this case - generate RETURN instruction
      if (branchInstr.getOpcode().toString().equals("RETURN")) {
        // JVM constructors ALWAYS return void, regardless of EK9 IR semantics
        if (isConstructor) {
          methodVisitor.visitInsn(RETURN); // Constructor always returns void in JVM
        } else {
          // Regular methods: check if we need to return a value based on IR
          final var returnType = branchInstr.getReturnValue();
          if (returnType != null && !returnType.isEmpty()) {
            // Load return value onto stack
            if (!"this".equals(returnType)) {
              // Load from variable
              final var varIndex = Math.abs(returnType.hashCode() % 10) + 1;
              methodVisitor.visitVarInsn(ALOAD, varIndex);
            } else {
              // Load 'this'
              methodVisitor.visitVarInsn(ALOAD, 0);
            }
            methodVisitor.visitInsn(ARETURN); // Return object reference
          } else {
            methodVisitor.visitInsn(RETURN); // Return void
          }
        }
      } else {
        // Other branch instructions would need more sophisticated handling
        System.err.println("WARNING: Unhandled branch type: " + branchInstr.getOpcode());
      }
    }
  }

  /**
   * Extract simple class name from fully qualified name.
   */
  private String getSimpleClassName(final String fullyQualifiedName) {
    final var lastIndex = fullyQualifiedName.lastIndexOf("::");
    return lastIndex >= 0 ? fullyQualifiedName.substring(lastIndex + 2) : fullyQualifiedName;
  }

  private void generateRegistryField() {
    classWriter.visitField(
        ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
        "REGISTRY",
        "Ljava/util/Map;",
        "Ljava/util/Map<Ljava/lang/String;Lek9/ProgramMetadata;>;",
        null
    ).visitEnd();
  }

  private void generateStaticInitializer() {
    MethodVisitor mv = classWriter.visitMethod(
        ACC_STATIC,
        "<clinit>",
        "()V",
        null,
        null
    );
    mv.visitCode();

    // REGISTRY = createRegistry()
    mv.visitMethodInsn(INVOKESTATIC, "ek9/Main", "createRegistry", "()Ljava/util/Map;", false);
    mv.visitFieldInsn(PUTSTATIC, "ek9/Main", "REGISTRY", "Ljava/util/Map;");

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void generateMainMethod() {
    MethodVisitor mv = classWriter.visitMethod(
        ACC_PUBLIC | ACC_STATIC,
        "main",
        "([Ljava/lang/String;)V",
        null,
        null
    );
    mv.visitCode();

    // Load REGISTRY
    mv.visitFieldInsn(GETSTATIC, "ek9/Main", "REGISTRY", "Ljava/util/Map;");

    // Load args
    mv.visitVarInsn(ALOAD, 0);

    // Create System.err::println lambda
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitInsn(DUP);
    mv.visitInvokeDynamicInsn(
        "accept",
        "(Ljava/io/PrintStream;)Ljava/util/function/Consumer;",
        new org.objectweb.asm.Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        ),
        new Object[]{
            org.objectweb.asm.Type.getType("(Ljava/lang/Object;)V"),
            new org.objectweb.asm.Handle(H_INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false),
            org.objectweb.asm.Type.getType("(Ljava/lang/String;)V")
        }
    );

    // Create System::exit lambda
    mv.visitInvokeDynamicInsn(
        "accept",
        "()Ljava/util/function/IntConsumer;",
        new org.objectweb.asm.Handle(
            H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;",
            false
        ),
        new Object[]{
            org.objectweb.asm.Type.getType("(I)V"),
            new org.objectweb.asm.Handle(H_INVOKESTATIC, "java/lang/System", "exit", "(I)V", false),
            org.objectweb.asm.Type.getType("(I)V")
        }
    );

    // Push "ek9.Main" string
    mv.visitLdcInsn("ek9.Main");

    // Call ProgramLauncher.launch
    mv.visitMethodInsn(
        INVOKESTATIC,
        "ek9/ProgramLauncher",
        "launch",
        "(Ljava/util/Map;[Ljava/lang/String;Ljava/util/function/Consumer;Ljava/util/function/IntConsumer;Ljava/lang/String;)V",
        false
    );

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void generateCreateRegistryMethodFromIR() {
    if (programEntryPoint == null) {
      throw new CompilerException("Cannot generate registry without ProgramEntryPointInstr");
    }

    MethodVisitor mv = classWriter.visitMethod(
        ACC_PRIVATE | ACC_STATIC,
        "createRegistry",
        "()Ljava/util/Map;",
        "()Ljava/util/Map<Ljava/lang/String;Lek9/ProgramMetadata;>;",
        null
    );
    mv.visitCode();

    // new HashMap()
    mv.visitTypeInsn(NEW, "java/util/HashMap");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
    mv.visitVarInsn(ASTORE, 0);

    // Process each program from ProgramEntryPointInstr
    for (var programDetails : programEntryPoint.getAvailablePrograms()) {
      // registry.put(qualifiedName, new ProgramMetadata(...))
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn(programDetails.qualifiedName());

      // new ProgramMetadata(...)
      mv.visitTypeInsn(NEW, "ek9/ProgramMetadata");
      mv.visitInsn(DUP);
      mv.visitLdcInsn(programDetails.qualifiedName());

      // Create parameter types array
      var paramTypes = programDetails.parameterSignature();
      mv.visitLdcInsn(paramTypes.size());
      mv.visitTypeInsn(ANEWARRAY, "java/lang/String");

      for (int i = 0; i < paramTypes.size(); i++) {
        mv.visitInsn(DUP);
        mv.visitLdcInsn(i);
        mv.visitLdcInsn(paramTypes.get(i).type());
        mv.visitInsn(AASTORE);
      }

      // Generate method name from qualified name
      String methodName = generateMethodNameFromQualifiedName(programDetails.qualifiedName());
      mv.visitLdcInsn(methodName);

      mv.visitMethodInsn(
          INVOKESPECIAL,
          "ek9/ProgramMetadata",
          "<init>",
          "(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;)V",
          false
      );

      mv.visitMethodInsn(
          INVOKEINTERFACE,
          "java/util/Map",
          "put",
          "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;",
          true
      );
      mv.visitInsn(POP);

      // Remember this mapping for method generation
      programMethodNames.put(programDetails.qualifiedName(), methodName);
    }

    // return registry
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private String generateMethodNameFromQualifiedName(String qualifiedName) {
    // Convert "introduction1::HelloWorld" to "execute_introduction1_HelloWorld"
    // This ensures uniqueness across modules
    String safeName = qualifiedName.replace("::", "_");
    return "execute_" + safeName;
  }

  private void generateProgramMethods(IRConstruct construct) {
    if (classWriter == null) {
      return; // No Main class to add methods to
    }

    // Find the _main method operation in this construct
    for (var operation : construct.getOperations()) {
      String operationName = operation.getSymbol().getName();

      if ("_main".equals(operationName)) {
        // This is the main program logic - generate execution method
        String qualifiedName = construct.getFullyQualifiedName();
        String methodName = generateMethodNameFromQualifiedName(qualifiedName);
        generateProgramExecutionMethod(methodName, operation, construct);
      }
    }
  }

  private void generateProgramExecutionMethod(String methodName,
                                            org.ek9lang.compiler.ir.instructions.OperationInstr operation,
                                            IRConstruct construct) {
    // Get method signature from the operation
    var symbol = operation.getSymbol();
    String methodDescriptor = generateMethodDescriptor(symbol);

    MethodVisitor mv = classWriter.visitMethod(
        ACC_PUBLIC | ACC_STATIC,
        methodName,
        methodDescriptor,
        null,
        null
    );
    mv.visitCode();

    // Process the operation's basic block to generate bytecode
    var basicBlock = operation.getBody();
    if (basicBlock != null) {
      processBasicBlock(mv, basicBlock);
    }

    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private String generateMethodDescriptor(org.ek9lang.compiler.symbols.ISymbol symbol) {
    // For now, simple implementation - will need to process actual parameter types
    if (symbol instanceof org.ek9lang.compiler.symbols.MethodSymbol methodSymbol) {
      var callParameters = methodSymbol.getCallParameters();
      if (!callParameters.isEmpty()) {
        // Has parameters - need to build descriptor based on parameter types
        StringBuilder desc = new StringBuilder("(");
        for (var param : callParameters) {
          desc.append("Lorg/ek9/lang/String;"); // Simplified - should map actual types
        }
        desc.append(")V");
        return desc.toString();
      }
    }
    return "()V"; // No parameters, void return
  }

  private void processBasicBlock(MethodVisitor mv,
                               org.ek9lang.compiler.ir.instructions.BasicBlockInstr basicBlock) {
    // Local variable counter
    int localVarIndex = 0;
    Map<String, Integer> variableMap = new HashMap<>();

    // Process each IR instruction in the basic block
    for (var instruction : basicBlock.getInstructions()) {
      localVarIndex = processIRInstruction(mv, instruction, variableMap, localVarIndex);
    }

    // If no explicit return was generated, add one
    if (basicBlock.getLastInstruction() == null ||
        !basicBlock.getLastInstruction().getOpcode().toString().equals("RETURN")) {
      mv.visitInsn(RETURN);
    }
  }

  private int processIRInstruction(MethodVisitor mv,
                                 org.ek9lang.compiler.ir.instructions.IRInstr instruction,
                                 Map<String, Integer> variableMap,
                                 int localVarIndex) {
    String opcode = instruction.getOpcode().toString();

    switch (opcode) {
      case "CALL":
        return processCallInstruction(mv, instruction, variableMap, localVarIndex);
      case "LOAD_LITERAL":
        return processLoadLiteralInstruction(mv, instruction, variableMap, localVarIndex);
      case "STORE":
        return processStoreInstruction(mv, instruction, variableMap, localVarIndex);
      case "LOAD":
        return processLoadInstruction(mv, instruction, variableMap, localVarIndex);
      case "REFERENCE":
        return processReferenceInstruction(mv, instruction, variableMap, localVarIndex);
      case "RETURN":
        mv.visitInsn(RETURN);
        return localVarIndex;
      case "SCOPE_ENTER":
      case "SCOPE_EXIT":
      case "RETAIN":
      case "SCOPE_REGISTER":
        // JVM handles scope and memory management automatically - ignore these
        return localVarIndex;
      default:
        System.err.println("WARNING: Unhandled IR instruction: " + opcode);
        return localVarIndex;
    }
  }

  private int processCallInstruction(MethodVisitor mv,
                                   org.ek9lang.compiler.ir.instructions.IRInstr instruction,
                                   Map<String, Integer> variableMap,
                                   int localVarIndex) {
    // Parse the CALL instruction - this is simplified, real implementation needs full parsing
    String instrStr = instruction.toString();

    if (instrStr.contains("org.ek9.lang::Stdout.<init>()")) {
      // Constructor call: new Stdout()
      mv.visitTypeInsn(NEW, "org/ek9/lang/Stdout");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "org/ek9/lang/Stdout", "<init>", "()V", false);

      // Store result if there's a target variable
      String resultVar = extractResultVariable(instrStr);
      if (resultVar != null) {
        variableMap.put(resultVar, localVarIndex);
        mv.visitVarInsn(ASTORE, localVarIndex);
        localVarIndex++;
      }
    } else if (instrStr.contains("println(")) {
      // Method call: stdout.println(toOutput)
      String objectVar = extractObjectVariable(instrStr);
      String paramVar = extractParameterVariable(instrStr);

      if (objectVar != null && variableMap.containsKey(objectVar)) {
        mv.visitVarInsn(ALOAD, variableMap.get(objectVar));
      }
      if (paramVar != null && variableMap.containsKey(paramVar)) {
        mv.visitVarInsn(ALOAD, variableMap.get(paramVar));
      }

      mv.visitMethodInsn(INVOKEVIRTUAL, "org/ek9/lang/Stdout", "println",
                        "(Lorg/ek9/lang/String;)Lorg/ek9/lang/Void;", false);
      mv.visitInsn(POP); // Discard return value
    }

    return localVarIndex;
  }

  private int processLoadLiteralInstruction(MethodVisitor mv,
                                          org.ek9lang.compiler.ir.instructions.IRInstr instruction,
                                          Map<String, Integer> variableMap,
                                          int localVarIndex) {
    String instrStr = instruction.toString();

    // Extract literal value and type - simplified parsing
    if (instrStr.contains("LOAD_LITERAL") && instrStr.contains("org.ek9.lang::String")) {
      String literal = extractStringLiteral(instrStr);
      String resultVar = extractResultVariable(instrStr);

      // Create EK9 String object with literal value
      mv.visitTypeInsn(NEW, "org/ek9/lang/String");
      mv.visitInsn(DUP);
      mv.visitLdcInsn(literal);
      mv.visitMethodInsn(INVOKESPECIAL, "org/ek9/lang/String", "<init>", "(Ljava/lang/String;)V", false);

      if (resultVar != null) {
        variableMap.put(resultVar, localVarIndex);
        mv.visitVarInsn(ASTORE, localVarIndex);
        localVarIndex++;
      }
    }

    return localVarIndex;
  }

  private int processStoreInstruction(MethodVisitor mv,
                                    org.ek9lang.compiler.ir.instructions.IRInstr instruction,
                                    Map<String, Integer> variableMap,
                                    int localVarIndex) {
    String instrStr = instruction.toString();
    String targetVar = extractTargetVariable(instrStr);
    String sourceVar = extractSourceVariable(instrStr);

    if (targetVar != null && sourceVar != null && variableMap.containsKey(sourceVar)) {
      // Copy from source to target variable
      mv.visitVarInsn(ALOAD, variableMap.get(sourceVar));

      if (!variableMap.containsKey(targetVar)) {
        variableMap.put(targetVar, localVarIndex++);
      }
      mv.visitVarInsn(ASTORE, variableMap.get(targetVar));
    }

    return localVarIndex;
  }

  private int processLoadInstruction(MethodVisitor mv,
                                   org.ek9lang.compiler.ir.instructions.IRInstr instruction,
                                   Map<String, Integer> variableMap,
                                   int localVarIndex) {
    String instrStr = instruction.toString();
    String sourceVar = extractSourceVariable(instrStr);
    String resultVar = extractResultVariable(instrStr);

    if (sourceVar != null && resultVar != null && variableMap.containsKey(sourceVar)) {
      mv.visitVarInsn(ALOAD, variableMap.get(sourceVar));

      if (!variableMap.containsKey(resultVar)) {
        variableMap.put(resultVar, localVarIndex++);
      }
      mv.visitVarInsn(ASTORE, variableMap.get(resultVar));
    }

    return localVarIndex;
  }

  private int processReferenceInstruction(MethodVisitor mv,
                                        org.ek9lang.compiler.ir.instructions.IRInstr instruction,
                                        Map<String, Integer> variableMap,
                                        int localVarIndex) {
    // REFERENCE just declares a variable - reserve space in variable map
    String instrStr = instruction.toString();
    String varName = extractVariableName(instrStr);

    if (varName != null && !variableMap.containsKey(varName)) {
      variableMap.put(varName, localVarIndex++);
    }

    return localVarIndex;
  }

  // Simplified instruction parsing methods - these would need to be more robust
  private String extractResultVariable(String instrStr) {
    if (instrStr.contains(" = ")) {
      return instrStr.substring(0, instrStr.indexOf(" = ")).trim();
    }
    return null;
  }

  private String extractStringLiteral(String instrStr) {
    if (instrStr.contains("\"") && instrStr.lastIndexOf("\"") > instrStr.indexOf("\"")) {
      return instrStr.substring(instrStr.indexOf("\"") + 1, instrStr.lastIndexOf("\""));
    }
    return "";
  }

  private String extractObjectVariable(String instrStr) {
    // Extract object from "obj.method()" pattern
    if (instrStr.contains(".") && instrStr.contains("(")) {
      String beforeDot = instrStr.substring(0, instrStr.indexOf("."));
      if (beforeDot.contains(" ")) {
        return beforeDot.substring(beforeDot.lastIndexOf(" ") + 1).trim();
      }
    }
    return null;
  }

  private String extractParameterVariable(String instrStr) {
    // Extract parameter from "method(param)" pattern
    if (instrStr.contains("(") && instrStr.contains(")")) {
      String params = instrStr.substring(instrStr.indexOf("(") + 1, instrStr.lastIndexOf(")"));
      return params.trim();
    }
    return null;
  }

  private String extractTargetVariable(String instrStr) {
    // For "STORE target, source"
    if (instrStr.startsWith("STORE ") && instrStr.contains(", ")) {
      return instrStr.substring(6, instrStr.indexOf(", ")).trim();
    }
    return null;
  }

  private String extractSourceVariable(String instrStr) {
    // For "STORE target, source" or "LOAD source"
    if (instrStr.contains(", ")) {
      return instrStr.substring(instrStr.indexOf(", ") + 2).trim();
    } else if (instrStr.startsWith("LOAD ")) {
      return instrStr.substring(5).trim();
    }
    return null;
  }

  private String extractVariableName(String instrStr) {
    // For "REFERENCE varName, type"
    if (instrStr.startsWith("REFERENCE ") && instrStr.contains(", ")) {
      return instrStr.substring(10, instrStr.indexOf(", ")).trim();
    }
    return null;
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
