package org.ek9lang.compiler.backend.jvm;

import java.util.function.Function;
import org.ek9lang.compiler.ir.data.ProgramDetails;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Generates the unified ek9.Main class that provides a standard Java entry point
 * for executing EK9 programs with automatic discovery and reflection-based invocation.
 */
public final class MainClassGenerator implements Function<ProgramEntryPointInstr, byte[]>, Opcodes {

  private final ClassWriter classWriter;
  private final EK9TypeToJVMDescriptor typeConverter = new EK9TypeToJVMDescriptor();

  public MainClassGenerator() {

    this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
  }

  @Override
  public byte[] apply(final ProgramEntryPointInstr programEntryPointInstr) {
    // Create class definition: public class ek9.Main
    classWriter.visit(V25, ACC_PUBLIC, "ek9/Main", null, "java/lang/Object", null);

    // Generate main method: public static void main(String[] args)
    generateMainMethod(programEntryPointInstr);

    classWriter.visitEnd();
    return classWriter.toByteArray();
  }

  /**
   * Generate the main(String[] args) method that uses ProgramLauncher for -r flag parsing.
   */
  private void generateMainMethod(final ProgramEntryPointInstr programEntryPointInstr) {
    final MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC | ACC_STATIC, "main",
        "([Ljava/lang/String;)V", null, new String[] {"java/lang/Exception"});
    mv.visitCode();

    // Simplified approach: Generate inline parsing logic instead of using ProgramLauncher for now
    // This avoids lambda metafactory issues

    // Check if we have any programs
    final var programs = programEntryPointInstr.getAvailablePrograms();
    if (programs.isEmpty()) {
      // No programs available
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
      mv.visitLdcInsn("No EK9 programs available");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
      mv.visitInsn(RETURN);
    } else if (programs.size() == 1) {
      // Single program - check for -r flag or execute directly
      generateSingleProgramLogic(mv, programs.getFirst());
    } else {
      // Multiple programs - require -r flag
      generateMultipleProgramLogic(mv, programs);
    }

    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  /**
   * Build method descriptor for _main method call.
   */
  private String buildMainMethodDescriptor(
      final java.util.List<org.ek9lang.compiler.ir.data.ParameterDetails> parameterSignature) {
    final var descriptor = new StringBuilder("(");

    for (var param : parameterSignature) {
      // Convert EK9 type to JVM descriptor
      descriptor.append(typeConverter.apply(param.type()));
    }

    descriptor.append(")V"); // _main returns void
    return descriptor.toString();
  }


  /**
   * Convert EK9 qualified name to Java class name format.
   * EK9: "package::ClassName" → Java: "package.ClassName"
   */
  private String convertToJavaClassName(final String qualifiedName) {
    return qualifiedName.replace("::", ".");
  }

  /**
   * Generate logic for single program execution (with optional -r flag support).
   */
  private void generateSingleProgramLogic(final MethodVisitor mv, final ProgramDetails program) {
    // For single program, accept both -r flag and direct execution
    // if (args.length >= 1 && "-r".equals(args[0])) {
    //   if (args.length < 2) { show usage }
    //   else if (args[1].equals(programName)) { execute }
    //   else { show "program not found" }
    // } else { execute directly }

    org.objectweb.asm.Label directExecution = new org.objectweb.asm.Label();
    org.objectweb.asm.Label showUsage = new org.objectweb.asm.Label();
    org.objectweb.asm.Label programNotFound = new org.objectweb.asm.Label();

    // Check if args.length >= 1
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IFEQ, directExecution); // if length == 0, go to direct execution

    // Check if args[0].equals("-r")
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_0);    // Index 0
    mv.visitInsn(AALOAD);      // args[0]
    mv.visitLdcInsn("-r");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
    mv.visitJumpInsn(IFEQ, directExecution); // if not equals, go to direct execution

    // We have -r flag, check if we have program name
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ARRAYLENGTH);
    mv.visitInsn(ICONST_2);
    mv.visitJumpInsn(IF_ICMPLT, showUsage); // if length < 2, show usage

    // Check if program name matches
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_1);    // Index 1
    mv.visitInsn(AALOAD);      // args[1]
    mv.visitLdcInsn(program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
    mv.visitJumpInsn(IFEQ, programNotFound); // if not equals, show error

    // Execute the program (with arguments from args[2..])
    generateDirectProgramCall(mv, program, 2); // Skip -r and program name
    mv.visitInsn(RETURN);

    // Label: showUsage
    mv.visitLabel(showUsage);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Usage: java ek9.Main -r <program-name> [arguments...]");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("  " + program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitInsn(RETURN);

    // Label: programNotFound
    mv.visitLabel(programNotFound);
    // Load the actual program name that was provided (args[1])
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Program '");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_1);    // Index 1
    mv.visitInsn(AALOAD);      // args[1] - the actual program name provided
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitLdcInsn("' not found. Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("  " + program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitInsn(RETURN);

    // Label: directExecution
    mv.visitLabel(directExecution);
    generateDirectProgramCall(mv, program, 0); // Use all args
  }

  /**
   * Generate logic for multiple program execution (requires -r flag).
   */
  private void generateMultipleProgramLogic(final MethodVisitor mv, final java.util.List<ProgramDetails> programs) {
    // Check args.length >= 2 && "-r".equals(args[0])
    org.objectweb.asm.Label showUsage = new org.objectweb.asm.Label();

    // Check args.length >= 2
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ARRAYLENGTH);
    mv.visitInsn(ICONST_2);
    mv.visitJumpInsn(IF_ICMPLT, showUsage);

    // Check args[0].equals("-r")
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_0);    // Index 0
    mv.visitInsn(AALOAD);      // args[0]
    mv.visitLdcInsn("-r");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
    mv.visitJumpInsn(IFEQ, showUsage);

    // Check program name against each available program
    for (int i = 0; i < programs.size(); i++) {
      final var program = programs.get(i);
      org.objectweb.asm.Label nextProgram = new org.objectweb.asm.Label();

      // if (args[1].equals(programName))
      mv.visitVarInsn(ALOAD, 0); // Load args
      mv.visitInsn(ICONST_1);    // Index 1
      mv.visitInsn(AALOAD);      // args[1]
      mv.visitLdcInsn(program.qualifiedName());
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "equals", "(Ljava/lang/Object;)Z", false);
      mv.visitJumpInsn(IFEQ, nextProgram);

      // Execute this program
      generateDirectProgramCall(mv, program, 2);
      mv.visitInsn(RETURN);

      mv.visitLabel(nextProgram);
    }

    // Program not found - show the actual program name that was provided
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Program '");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_1);    // Index 1
    mv.visitInsn(AALOAD);      // args[1] - the actual program name provided
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitLdcInsn("' not found. Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    for (ProgramDetails program : programs) {
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
      mv.visitLdcInsn("  " + program.qualifiedName());
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
    mv.visitInsn(RETURN);

    // Label: showUsage
    mv.visitLabel(showUsage);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Usage: java ek9.Main -r <program-name> [arguments...]");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitLdcInsn("Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);

    for (ProgramDetails program : programs) {
      mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
      mv.visitLdcInsn("  " + program.qualifiedName());
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    }
  }

  /**
   * Generate direct program instantiation and _main call with argument conversion.
   * Converts command-line String arguments to EK9 types and calls _main.
   */
  private void generateDirectProgramCall(final MethodVisitor mv, final ProgramDetails program,
      final int argsOffset) {
    final var parameterSignature = program.parameterSignature();

    // Validate argument count matches parameter count
    // if (args.length - argsOffset != parameterSignature.size()) { show error and return }
    org.objectweb.asm.Label argumentCountValid = new org.objectweb.asm.Label();

    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ARRAYLENGTH);
    mv.visitLdcInsn(argsOffset);
    mv.visitInsn(ISUB); // args.length - argsOffset
    mv.visitLdcInsn(parameterSignature.size());
    mv.visitJumpInsn(IF_ICMPEQ, argumentCountValid);

    // Argument count mismatch - show error
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Error: Program '");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V",
        false);
    mv.visitLdcInsn(program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitLdcInsn("' requires ");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitLdcInsn(parameterSignature.size());
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(I)Ljava/lang/StringBuilder;", false);
    mv.visitLdcInsn(" argument(s), but ");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitLdcInsn(argsOffset);
    mv.visitInsn(ISUB);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(I)Ljava/lang/StringBuilder;", false);
    mv.visitLdcInsn(" provided");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append",
        "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;",
        false);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
    mv.visitInsn(RETURN);

    // Label: argumentCountValid
    mv.visitLabel(argumentCountValid);

    // Create instance of the EK9 program class
    final var javaClassName = convertToJavaClassName(program.qualifiedName());
    final var internalClassName = javaClassName.replace(".", "/");

    // new ProgramClass()
    mv.visitTypeInsn(NEW, internalClassName);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, internalClassName, "<init>", "()V", false);

    // Store instance in local variable (after converting parameters)
    final int instanceLocalVar = 1 + parameterSignature.size();
    mv.visitVarInsn(ASTORE, instanceLocalVar);

    // Convert each command-line argument to EK9 type and store in local variables
    for (int i = 0; i < parameterSignature.size(); i++) {
      final var param = parameterSignature.get(i);
      final int paramLocalVar = 1 + i; // Start at local var 1 (0 is args array)

      // Load command-line argument: args[argsOffset + i]
      mv.visitVarInsn(ALOAD, 0); // Load args array
      mv.visitLdcInsn(argsOffset + i); // Index
      mv.visitInsn(AALOAD); // args[index] - Java String

      // Convert to EK9 type using factory method
      generateTypeConversion(mv, param.type());

      // Store converted EK9 object in local variable
      mv.visitVarInsn(ASTORE, paramLocalVar);
    }

    // Load instance and call _main with converted parameters
    mv.visitVarInsn(ALOAD, instanceLocalVar); // Load instance

    // Load each converted parameter
    for (int i = 0; i < parameterSignature.size(); i++) {
      mv.visitVarInsn(ALOAD, 1 + i); // Load converted parameter
    }

    // Build _main method descriptor and call
    final var mainMethodDescriptor = buildMainMethodDescriptor(parameterSignature);
    mv.visitMethodInsn(INVOKEVIRTUAL, internalClassName, "_main", mainMethodDescriptor, false);
  }

  /**
   * Generate bytecode to convert a Java String to an EK9 type using factory method.
   * Stack: [javaString] -> [ek9Object]
   * Supports all types defined in ProgramArgumentPredicate.
   */
  private void generateTypeConversion(final MethodVisitor mv, final String ek9Type) {
    // Call appropriate EK9 factory method: Type._of(javaString)
    // All these types support _of(String) factory method for command-line conversion
    switch (ek9Type) {
      // Basic types
      case "org.ek9.lang::String" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/String", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/String;", false);
      case "org.ek9.lang::Integer" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Integer", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Integer;", false);
      case "org.ek9.lang::Float" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Float", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Float;", false);
      case "org.ek9.lang::Boolean" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Boolean", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Boolean;", false);
      case "org.ek9.lang::Character" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Character",
          "_of", "(Ljava/lang/String;)Lorg/ek9/lang/Character;", false);
      case "org.ek9.lang::Bits" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Bits", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Bits;", false);

      // Date/Time types
      case "org.ek9.lang::Date" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Date", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Date;", false);
      case "org.ek9.lang::DateTime" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/DateTime",
          "_of", "(Ljava/lang/String;)Lorg/ek9/lang/DateTime;", false);
      case "org.ek9.lang::Time" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Time", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Time;", false);
      case "org.ek9.lang::Duration" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Duration",
          "_of", "(Ljava/lang/String;)Lorg/ek9/lang/Duration;", false);
      case "org.ek9.lang::Millisecond" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Millisecond",
          "_of", "(Ljava/lang/String;)Lorg/ek9/lang/Millisecond;", false);

      // Physical/Visual types
      case "org.ek9.lang::Dimension" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Dimension",
          "_of", "(Ljava/lang/String;)Lorg/ek9/lang/Dimension;", false);
      case "org.ek9.lang::Resolution" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Resolution",
          "_of", "(Ljava/lang/String;)Lorg/ek9/lang/Resolution;", false);
      case "org.ek9.lang::Colour" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Colour", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Colour;", false);

      // Financial/Pattern types
      case "org.ek9.lang::Money" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/Money", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/Money;", false);
      case "org.ek9.lang::RegEx" -> mv.visitMethodInsn(INVOKESTATIC, "org/ek9/lang/RegEx", "_of",
          "(Ljava/lang/String;)Lorg/ek9/lang/RegEx;", false);

      default -> {
        // Check if this is List of String (generic type)
        if (ek9Type.startsWith("_List_") && ek9Type.contains("String")) {
          // List of String - needs special handling for multiple arguments
          // For now, this is unsupported in simple command-line parsing
          mv.visitInsn(POP); // Remove javaString from stack
          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
          mv.visitLdcInsn(
              "Error: List of String parameters not yet supported in command-line execution");
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V",
              false);
          mv.visitInsn(RETURN);
        } else {
          // Unsupported type - show error and exit
          mv.visitInsn(POP); // Remove javaString from stack
          mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
          mv.visitLdcInsn("Error: Unsupported parameter type: " + ek9Type);
          mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V",
              false);
          mv.visitInsn(RETURN);
        }
      }
    }
    // TODO: Add validation that conversion succeeded (_isSet() check) in future enhancement
  }
}