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
import org.ek9lang.compiler.ir.instructions.BranchInstr;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRConstruct;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.OperationInstr;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
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
    // Disable automatic frame computation to avoid stack verification issues during development
    classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);

    // Get the actual program class name from the IR construct
    final var programClassName = construct.getFullyQualifiedName();
    final var jvmClassName = jvmNameConverter.apply(programClassName);

    classWriter.visit(V21, ACC_PUBLIC, jvmClassName, null, JAVA_LANG_OBJECT, null);

    // Add source file information for debugging - uses actual .ek9 source filename from IR
    classWriter.visitSource(construct.getSourceFileName(), null);
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
      processBasicBlockWithTypedInstructions(mv, basicBlock);
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
      processBasicBlockWithTypedInstructions(mv, basicBlock);
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
  private void generateMainMethodFromIR(final OperationInstr operation) {
    // Build method descriptor from operation signature
    final var methodDescriptor = buildMethodDescriptor(operation);

    final var mv = classWriter.visitMethod(ACC_PUBLIC, METHOD_MAIN, methodDescriptor, null, null);
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
  private List<String> getParameterNamesForMethod() {
    if (programEntryPoint != null) {
      final var programClassName = getProgramClassName();

      // Find matching program in entry point
      for (var programDetails : programEntryPoint.getAvailablePrograms()) {
        if (programDetails.qualifiedName().equals(programClassName)) {
          // Extract parameter names from parameter signature
          return programDetails.parameterSignature().stream()
              .map(ParameterDetails::name)
              .toList();
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
   * Process a basic block using typed IR instructions via visitor pattern.
   * Uses specialized ASM generators for each instruction type.
   */
  private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                      final BasicBlockInstr basicBlock) {
    processInstructions(new InstructionVisitor(mv), basicBlock);
  }

  /**
   * Overloaded version with parameter names for _main method context.
   * Pre-registers parameters in variable map to prevent null overwriting.
   */
  private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                      final BasicBlockInstr basicBlock,
                                                      final List<String> parameterNames) {
    processInstructions(new InstructionVisitor(mv, parameterNames), basicBlock);
  }

  /**
   * Overloaded version for constructor context.
   */
  private void processBasicBlockWithTypedInstructions(final MethodVisitor mv,
                                                      final BasicBlockInstr basicBlock,
                                                      final boolean isConstructor) {
    processInstructions(new InstructionVisitor(mv, isConstructor), basicBlock);
  }

  /**
   * Common instruction processing logic used by all overloads.
   * Dispatches each IR instruction to the appropriate specialized generator.
   */
  private void processInstructions(final InstructionVisitor instructionVisitor,
                                   final BasicBlockInstr basicBlock) {
    // Process each typed IR instruction using visitor pattern
    for (var instruction : basicBlock.getInstructions()) {
      // Use visitor pattern to dispatch to appropriate specialized generator
      switch (instruction) {
        case CallInstr callInstr -> instructionVisitor.processCallInstruction(callInstr);
        case LiteralInstr literalInstr -> instructionVisitor.processLiteralInstruction(literalInstr);
        case MemoryInstr memoryInstr -> instructionVisitor.processMemoryInstruction(memoryInstr);
        case ScopeInstr scopeInstr -> instructionVisitor.processScopeInstruction(scopeInstr);
        case BranchInstr branchInstr -> instructionVisitor.processBranchInstruction(branchInstr);
        default -> throw new CompilerException(
            "WARNING: Unhandled typed IR instruction: " + instruction.getClass().getSimpleName());
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
    private final boolean isConstructor;

    InstructionVisitor(final MethodVisitor mv) {
      this(mv, false);
    }

    InstructionVisitor(final MethodVisitor mv, final boolean isConstructor) {
      this(mv, isConstructor, Collections.emptyList());
    }

    /**
     * Constructor with parameter names for pre-registration in variable map.
     * Parameters are allocated to local variable slots 1, 2, 3, ... (slot 0 is 'this').
     */
    InstructionVisitor(final MethodVisitor mv, final List<String> parameterNames) {
      this(mv, false, parameterNames);
    }

    private InstructionVisitor(final MethodVisitor mv, final boolean isConstructor,
                               final List<String> parameterNames) {
      this.methodVisitor = mv;
      this.isConstructor = isConstructor;

      // Cast visitor to OutputVisitor since that's what created this AsmStructureCreator
      final OutputVisitor outputVisitor = (OutputVisitor) visitor;

      // Create instances of existing specialized generators with proper OutputVisitor
      this.callInstrGenerator = new CallInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);
      this.literalInstrGenerator = new LiteralInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);
      this.memoryInstrGenerator = new MemoryInstrAsmGenerator(constructTargetTuple, outputVisitor, classWriter);

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

      // Set the current method visitor for all generators
      this.callInstrGenerator.setCurrentMethodVisitor(mv);
      this.literalInstrGenerator.setCurrentMethodVisitor(mv);
      this.memoryInstrGenerator.setCurrentMethodVisitor(mv);
    }

    void processCallInstruction(final CallInstr callInstr) {
      // Delegate to existing specialized CallInstrAsmGenerator
      callInstrGenerator.generateCall(callInstr);
    }

    void processLiteralInstruction(final LiteralInstr literalInstr) {
      // Delegate to existing specialized LiteralInstrAsmGenerator
      literalInstrGenerator.generateLiteral(literalInstr);
    }

    void processMemoryInstruction(final MemoryInstr memoryInstr) {
      // Delegate to existing specialized MemoryInstrAsmGenerator
      memoryInstrGenerator.generateMemoryOperation(memoryInstr);
    }

    void processScopeInstruction(final ScopeInstr scopeInstr) {
      //No-op
    }

    void processBranchInstruction(final BranchInstr branchInstr) {
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
        throw new CompilerException("WARNING: Unhandled branch type: " + branchInstr.getOpcode());
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
