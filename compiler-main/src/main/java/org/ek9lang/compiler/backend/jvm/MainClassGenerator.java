package org.ek9lang.compiler.backend.jvm;

import java.util.List;
import org.ek9lang.compiler.ir.data.ProgramDetails;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Generates the unified ek9.Main class that provides a standard Java entry point
 * for executing EK9 programs with automatic discovery and reflection-based invocation.
 */
public final class MainClassGenerator implements Opcodes {

  private final List<ProgramDetails> programs;
  private final ClassWriter classWriter;

  public MainClassGenerator(final List<ProgramDetails> programs) {
    AssertValue.checkNotNull("Programs list cannot be null", programs);
    this.programs = programs;
    this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
  }

  /**
   * Generate the complete ek9.Main class bytecode.
   */
  public byte[] generateMainClass() {
    // Create class definition: public class ek9.Main
    classWriter.visit(V25, ACC_PUBLIC, "ek9/Main", null, "java/lang/Object", null);

    // Generate main method: public static void main(String[] args)
    generateMainMethod();

    // Generate helper methods
    generateListProgramsMethod();
    generateExecuteProgramMethod();
    generateShowHelpMethod();

    classWriter.visitEnd();
    return classWriter.toByteArray();
  }

  /**
   * Generate the main(String[] args) method.
   */
  private void generateMainMethod() {
    final MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main",
        "([Ljava/lang/String;)V", null, new String[] {"java/lang/Exception"});
    mv.visitCode();

    // Handle different argument scenarios
    if (programs.size() == 1) {
      // Single program - execute it directly (ignore command line args for now)
      final var program = programs.get(0);
      generateExecuteProgram(mv, program);
    } else if (programs.size() > 1) {
      // Multiple programs - show help and list
      generateShowHelp(mv);
    } else {
      // No programs - show error
      generateNoProgramsError(mv);
    }

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate method to list available programs.
   */
  private void generateListProgramsMethod() {
    final MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "listPrograms",
        "()V", null, null);
    mv.visitCode();

    // System.out.println("Available programs:");
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    // Print each program
    for (ProgramDetails program : programs) {
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
      mv.visitLdcInsn("  " + getSimpleClassName(program.qualifiedName()));
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate method to execute a specific program by name.
   */
  private void generateExecuteProgramMethod() {
    final MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "executeProgram",
        "(Ljava/lang/String;)V", null, new String[] {"java/lang/Exception"});
    mv.visitCode();

    // TODO: Implement program selection and reflection-based execution
    // For now, just show a placeholder message
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Program execution not yet implemented");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate method to show help information.
   */
  private void generateShowHelpMethod() {
    final MethodVisitor mv = classWriter.visitMethod(ACC_PRIVATE | ACC_STATIC, "showHelp",
        "()V", null, null);
    mv.visitCode();

    // System.out.println("Usage: java ek9.Main [ProgramName] [args...]");
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Usage: java ek9.Main [ProgramName] [args...]");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Generate code to execute a specific program (for single program case).
   */
  private void generateExecuteProgram(final MethodVisitor mv, final ProgramDetails program) {
    // System.out.println("Executing program: " + programName);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Executing EK9 program: " + getSimpleClassName(program.qualifiedName()));
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    // Generate reflection-based program execution similar to Driver.java
    // Class<?> clazz = Class.forName("packageName.ClassName");
    mv.visitLdcInsn(convertToJavaClassName(program.qualifiedName()));
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Class", "forName", "(Ljava/lang/String;)Ljava/lang/Class;", false);
    mv.visitVarInsn(ASTORE, 1); // Store Class object in local variable 1

    // Constructor constructor = clazz.getDeclaredConstructor();
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredConstructor",
        "([Ljava/lang/Class;)Ljava/lang/reflect/Constructor;", false);
    mv.visitVarInsn(ASTORE, 2); // Store Constructor in local variable 2

    // Object instance = constructor.newInstance();
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Constructor", "newInstance",
        "([Ljava/lang/Object;)Ljava/lang/Object;", false);
    mv.visitVarInsn(ASTORE, 3); // Store instance in local variable 3

    // Method mainMethod = clazz.getDeclaredMethod("_main");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("_main");
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
        "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
    mv.visitVarInsn(ASTORE, 4); // Store Method in local variable 4

    // mainMethod.invoke(instance);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ICONST_0);
    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke",
        "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
    mv.visitInsn(POP); // Discard return value

    // System.out.println("EK9 program completed successfully");
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("EK9 program completed successfully");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
  }

  /**
   * Generate code to show help information.
   */
  private void generateShowHelp(final MethodVisitor mv) {
    mv.visitMethodInsn(INVOKESTATIC, "ek9/Main", "showHelp", "()V", false);
    mv.visitMethodInsn(INVOKESTATIC, "ek9/Main", "listPrograms", "()V", false);
  }

  /**
   * Generate code to show "no programs available" error.
   */
  private void generateNoProgramsError(final MethodVisitor mv) {
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("No EK9 programs available");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
  }

  /**
   * Extract simple class name from fully qualified name.
   */
  private String getSimpleClassName(final String qualifiedName) {
    final int lastIndex = qualifiedName.lastIndexOf("::");
    return lastIndex >= 0 ? qualifiedName.substring(lastIndex + 2) : qualifiedName;
  }

  /**
   * Convert EK9 qualified name to Java class name format.
   * EK9: "package::ClassName" → Java: "package.ClassName"
   */
  private String convertToJavaClassName(final String qualifiedName) {
    return qualifiedName.replace("::", ".");
  }
}