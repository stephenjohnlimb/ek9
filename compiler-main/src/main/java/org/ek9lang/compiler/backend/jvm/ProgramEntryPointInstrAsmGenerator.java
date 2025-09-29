package org.ek9lang.compiler.backend.jvm;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.data.ParameterDetails;
import org.ek9lang.compiler.ir.data.ProgramDetails;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.SharedThreadContext;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for ProgramEntryPointInstr processing.
 * Handles generation of the main ek9.Main class with program selection logic.
 * Uses SharedThreadContext pattern for thread-safe coordination since only
 * one thread should generate the Main class.
 */
public final class ProgramEntryPointInstrAsmGenerator extends AbstractAsmGenerator {

  // Shared state for coordinating Main class generation across threads
  private static final SharedThreadContext<MainClassState> mainClassState =
      new SharedThreadContext<>(new MainClassState());

  public ProgramEntryPointInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                            final OutputVisitor outputVisitor,
                                            final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate main class entry point using thread-safe coordination.
   * Only one thread will actually generate the Main class bytecode.
   */
  public void generateProgramEntryPoint(final ProgramEntryPointInstr programInstr) {
    AssertValue.checkNotNull("ProgramEntryPointInstr cannot be null", programInstr);

    // Use SharedThreadContext pattern to ensure only one thread generates Main class
    mainClassState.accept(state -> {
      if (!state.isInitialized.getAndSet(true)) {
        // First thread to reach here generates the Main class
        generateMainClass(programInstr);
        state.availablePrograms = programInstr.getAvailablePrograms();
      }
      // Subsequent threads skip Main class generation
    });
  }

  /**
   * Generate the complete ek9.Main class with program selection logic.
   */
  private void generateMainClass(final ProgramEntryPointInstr programInstr) {
    final var availablePrograms = programInstr.getAvailablePrograms();

    // Create new ClassWriter for ek9.Main class
    final var mainClassWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

    // Define main class
    mainClassWriter.visit(
        Opcodes.V11, // Java 11 bytecode
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
        "ek9/Main",
        null,
        "java/lang/Object",
        null
    );

    // Generate main method
    generateMainMethod(mainClassWriter, availablePrograms);

    // Generate program execution methods
    generateProgramExecutionMethods(mainClassWriter, availablePrograms);

    mainClassWriter.visitEnd();

    // Store the generated Main class bytecode
    // In a full implementation, this would be written to the appropriate output file
    final var mainClassBytecode = mainClassWriter.toByteArray();

    System.out.println("Generated ek9.Main class with " + availablePrograms.size() + " programs");
  }

  /**
   * Generate the main(String[] args) method with program selection logic.
   */
  private void generateMainMethod(final ClassWriter mainClassWriter,
                                  final List<ProgramDetails> availablePrograms) {

    final var mainMethod = mainClassWriter.visitMethod(
        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
        "main",
        "([Ljava/lang/String;)V",
        null,
        null
    );

    mainMethod.visitCode();

    // Program selection logic based on command line arguments
    if (availablePrograms.size() == 1) {
      // Single program - execute directly
      final var program = availablePrograms.getFirst();
      generateSingleProgramExecution(mainMethod, program);
    } else {
      // Multiple programs - generate selection logic
      generateMultipleProgramSelection(mainMethod, availablePrograms);
    }

    mainMethod.visitInsn(Opcodes.RETURN);
    mainMethod.visitMaxs(0, 0); // Let ASM compute
    mainMethod.visitEnd();
  }

  /**
   * Generate execution for a single program case.
   */
  private void generateSingleProgramExecution(final MethodVisitor mainMethod,
                                              final ProgramDetails program) {

    final var programClassName = convertToJvmName(program.qualifiedName());

    // Create new instance of the program class
    mainMethod.visitTypeInsn(Opcodes.NEW, programClassName);
    mainMethod.visitInsn(Opcodes.DUP);

    // Call constructor
    if (program.parameterSignature().isEmpty()) {
      // No-argument constructor
      mainMethod.visitMethodInsn(
          Opcodes.INVOKESPECIAL,
          programClassName,
          "<init>",
          "()V",
          false
      );
    } else {
      // Constructor with parameters (parse from command line)
      generateParameterParsing(mainMethod, program.parameterSignature());
      final var constructorDescriptor = generateConstructorDescriptor(program.parameterSignature());
      mainMethod.visitMethodInsn(
          Opcodes.INVOKESPECIAL,
          programClassName,
          "<init>",
          constructorDescriptor,
          false
      );
    }

    // Call _main method
    mainMethod.visitMethodInsn(
        Opcodes.INVOKEVIRTUAL,
        programClassName,
        "_main",
        "()V",
        false
    );
  }

  /**
   * Generate program selection logic for multiple programs.
   */
  private void generateMultipleProgramSelection(final MethodVisitor mainMethod,
                                                final List<ProgramDetails> availablePrograms) {

    // For now, generate simple program listing and selection
    // Load System.out
    mainMethod.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mainMethod.visitLdcInsn("Available EK9 programs:");
    mainMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    // List each program
    for (int i = 0; i < availablePrograms.size(); i++) {
      final var program = availablePrograms.get(i);

      mainMethod.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      mainMethod.visitLdcInsn((i + 1) + ". " + program.qualifiedName());
      mainMethod.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V",
          false);
    }

    // For now, execute the first program by default
    if (!availablePrograms.isEmpty()) {
      generateSingleProgramExecution(mainMethod, availablePrograms.getFirst());
    }
  }

  /**
   * Generate parameter parsing from command line arguments.
   */
  private void generateParameterParsing(final MethodVisitor mainMethod,
                                        final List<ParameterDetails> parameters) {

    // Load command line args array
    mainMethod.visitVarInsn(Opcodes.ALOAD, 0);

    // For each parameter, parse from args array
    for (int i = 0; i < parameters.size(); i++) {
      final var param = parameters.get(i);

      // Load args[i] or default value if not enough args
      mainMethod.visitInsn(Opcodes.DUP); // Duplicate args array
      mainMethod.visitIntInsn(Opcodes.BIPUSH, i);

      // Check if we have enough arguments
      final var hasArgLabel = new org.objectweb.asm.Label();
      final var defaultLabel = new org.objectweb.asm.Label();

      mainMethod.visitLabel(hasArgLabel);
      mainMethod.visitLabel(defaultLabel);

      mainMethod.visitInsn(Opcodes.DUP2); // Duplicate array and index
      mainMethod.visitInsn(Opcodes.ARRAYLENGTH);
      mainMethod.visitJumpInsn(Opcodes.IF_ICMPLT, defaultLabel);

      // Has argument - load it
      mainMethod.visitInsn(Opcodes.AALOAD);
      mainMethod.visitJumpInsn(Opcodes.GOTO, hasArgLabel);

      // No argument - use default
      mainMethod.visitLabel(defaultLabel);
      mainMethod.visitInsn(Opcodes.POP2); // Clean up stack
      generateDefaultValue(mainMethod, param.type());

      mainMethod.visitLabel(hasArgLabel);
      // Convert string to appropriate type based on param.type()
      generateStringToTypeConversion(mainMethod, param.type());
    }

    mainMethod.visitInsn(Opcodes.POP); // Remove args array from stack
  }

  /**
   * Generate default value for a parameter type.
   */
  private void generateDefaultValue(final MethodVisitor mainMethod, final String paramType) {
    // For now, generate empty string as default
    mainMethod.visitLdcInsn("");
  }

  /**
   * Generate conversion from string to parameter type.
   */
  private void generateStringToTypeConversion(final MethodVisitor mainMethod, final String paramType) {
    // For now, assume all parameters are strings
    // In full implementation, would handle Integer, Boolean, etc.
  }

  /**
   * Generate constructor descriptor from parameter types.
   */
  private String generateConstructorDescriptor(final List<ParameterDetails> parameters) {
    final var descriptor = new StringBuilder("(");
    for (final var param : parameters) {
      descriptor.append(convertToJvmDescriptor(param.type()));
    }
    descriptor.append(")V");
    return descriptor.toString();
  }

  /**
   * Generate helper methods for program execution.
   */
  private void generateProgramExecutionMethods(final ClassWriter mainClassWriter,
                                               final List<ProgramDetails> availablePrograms) {
    // Could generate helper methods for program discovery, reflection, etc.
  }

  /**
   * Shared state for Main class generation coordination.
   */
  private static class MainClassState implements Serializable {
    private final AtomicBoolean isInitialized = new AtomicBoolean(false);
    private List<ProgramDetails> availablePrograms = List.of();

    // Additional state could be added here for more complex coordination
  }
}