package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.common.Ek9ExitCodes.PROGRAM_ARGUMENT_COUNT_MISMATCH_EXIT_CODE;
import static org.ek9lang.compiler.common.Ek9ExitCodes.PROGRAM_ARGUMENT_TYPE_MISMATCH_EXIT_CODE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BITS;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_COLOUR;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATE;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DATETIME;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DIMENSION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_DURATION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_LIST_OF_STRING;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MILLISECOND;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_MONEY;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_REGEX;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_RESOLUTION;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_TIME;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_INT_TO_STRING_BUILDER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_OBJECT_TO_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_PRINT_STREAM;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_ARRAY_TO_VOID;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_BITS;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_COLOUR;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_DATE;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_DATETIME;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_DIMENSION;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_DURATION;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_MILLISECOND;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_MONEY;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_REGEX;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_RESOLUTION;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_EK9_TIME;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_STRING_BUILDER;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_STRING_TO_VOID;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_VOID_TO_BOOLEAN_PRIMITIVE;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_VOID_TO_EK9_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_VOID_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_VOID_TO_VOID;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_BITS;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_BUILTIN_TYPE;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_COLOUR;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_DATE;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_DATETIME;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_DIMENSION;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_DURATION;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_MILLISECOND;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_MONEY;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_REGEX;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_RESOLUTION;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.EK9_LANG_TIME;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_IO_PRINT_STREAM;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_EXCEPTION;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_OBJECT;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_STRING_BUILDER;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_SYSTEM;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_UTIL_ARRAY_LIST;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_UTIL_LIST;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_INIT;
import static org.ek9lang.compiler.support.JVMTypeNames.METHOD_MAIN;
import static org.ek9lang.compiler.support.JVMTypeNames.PARAM_OBJECT;
import static org.ek9lang.compiler.support.JVMTypeNames.PARAM_UTIL_LIST;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.data.ParameterDetails;
import org.ek9lang.compiler.ir.data.ProgramDetails;
import org.ek9lang.compiler.ir.instructions.ProgramEntryPointInstr;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Generates the unified ek9.Main class that provides a standard Java entry point
 * for executing EK9 programs with automatic discovery and reflection-based invocation.
 */
final class MainClassGenerator implements Function<ProgramEntryPointInstr, byte[]>, Opcodes {

  private final ClassWriter classWriter;
  private final FullyQualifiedJvmName jvmNameConverter = new FullyQualifiedJvmName();
  private final JvmDescriptorConverter descriptorConverter = new JvmDescriptorConverter(jvmNameConverter);

  public MainClassGenerator() {

    this.classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
  }

  @Override
  public byte[] apply(final ProgramEntryPointInstr programEntryPointInstr) {
    // Create class definition: public class ek9.Main
    classWriter.visit(V25, ACC_PUBLIC, "ek9/Main", null, JAVA_LANG_OBJECT, null);

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
        DESC_STRING_ARRAY_TO_VOID, null, new String[] {JAVA_LANG_EXCEPTION});
    mv.visitCode();

    // Simplified approach: Generate inline parsing logic instead of using ProgramLauncher for now
    // This avoids lambda metafactory issues

    // Check if we have any programs
    final var programs = programEntryPointInstr.getAvailablePrograms();
    if (programs.isEmpty()) {
      // No programs available
      mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
      mv.visitLdcInsn("No EK9 programs available");
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
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
  private String buildMainMethodDescriptor(final List<ParameterDetails> parameterSignature) {
    final var descriptor = new StringBuilder("(");

    for (var param : parameterSignature) {
      // Convert EK9 type to JVM descriptor
      descriptor.append(descriptorConverter.apply(param.type()));
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
   * Extract simple type name from qualified type name for error messages.
   * EK9: "org.ek9.lang::Integer" → "Integer"
   */
  private String getSimpleTypeName(final String qualifiedName) {
    final int index = qualifiedName.lastIndexOf("::");
    return index >= 0 ? qualifiedName.substring(index + 2) : qualifiedName;
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

    Label directExecution = new Label();
    Label showUsage = new Label();
    Label programNotFound = new Label();

    // Check if args.length >= 1
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ARRAYLENGTH);
    mv.visitJumpInsn(IFEQ, directExecution); // if length == 0, go to direct execution

    // Check if args[0].equals("-r")
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_0);    // Index 0
    mv.visitInsn(AALOAD);      // args[0]
    mv.visitLdcInsn("-r");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING, "equals", DESC_OBJECT_TO_BOOLEAN, false);
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
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING, "equals", DESC_OBJECT_TO_BOOLEAN, false);
    mv.visitJumpInsn(IFEQ, programNotFound); // if not equals, show error

    // Execute the program (with arguments from args[2..])
    generateDirectProgramCall(mv, program, 2); // Skip -r and program name
    mv.visitInsn(RETURN);

    // Label: showUsage
    mv.visitLabel(showUsage);
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitLdcInsn("Usage: java ek9.Main -r <program-name> [arguments...]");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitLdcInsn("Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitLdcInsn("  " + program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitInsn(RETURN);

    // Label: programNotFound
    mv.visitLabel(programNotFound);
    // Load the actual program name that was provided (args[1])
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitTypeInsn(NEW, JAVA_LANG_STRING_BUILDER);
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Program '");
    mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_STRING_BUILDER, METHOD_INIT, DESC_STRING_TO_VOID, false);
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_1);    // Index 1
    mv.visitInsn(AALOAD);      // args[1] - the actual program name provided
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitLdcInsn("' not found. Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "toString", DESC_VOID_TO_STRING, false);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitLdcInsn("  " + program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitInsn(RETURN);

    // Label: directExecution
    mv.visitLabel(directExecution);
    generateDirectProgramCall(mv, program, 0); // Use all args
  }

  /**
   * Generate logic for multiple program execution (requires -r flag).
   */
  private void generateMultipleProgramLogic(final MethodVisitor mv, final List<ProgramDetails> programs) {
    // Check args.length >= 2 && "-r".equals(args[0])
    Label showUsage = new Label();

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
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING, "equals", DESC_OBJECT_TO_BOOLEAN, false);
    mv.visitJumpInsn(IFEQ, showUsage);

    // Check program name against each available program
    for (final ProgramDetails program : programs) {
      Label nextProgram = new Label();

      // if (args[1].equals(programName))
      mv.visitVarInsn(ALOAD, 0); // Load args
      mv.visitInsn(ICONST_1);    // Index 1
      mv.visitInsn(AALOAD);      // args[1]
      mv.visitLdcInsn(program.qualifiedName());
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING, "equals", DESC_OBJECT_TO_BOOLEAN, false);
      mv.visitJumpInsn(IFEQ, nextProgram);

      // Execute this program
      generateDirectProgramCall(mv, program, 2);
      mv.visitInsn(RETURN);

      mv.visitLabel(nextProgram);
    }

    // Program not found - show the actual program name that was provided
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitTypeInsn(NEW, JAVA_LANG_STRING_BUILDER);
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Program '");
    mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_STRING_BUILDER, METHOD_INIT, DESC_STRING_TO_VOID, false);
    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ICONST_1);    // Index 1
    mv.visitInsn(AALOAD);      // args[1] - the actual program name provided
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitLdcInsn("' not found. Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "toString", DESC_VOID_TO_STRING, false);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);

    for (ProgramDetails program : programs) {
      mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
      mv.visitLdcInsn("  " + program.qualifiedName());
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    }
    mv.visitInsn(RETURN);

    // Label: showUsage
    mv.visitLabel(showUsage);
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitLdcInsn("Usage: java ek9.Main -r <program-name> [arguments...]");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitLdcInsn("Available programs:");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);

    for (ProgramDetails program : programs) {
      mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
      mv.visitLdcInsn("  " + program.qualifiedName());
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    }
  }

  /**
   * Generate direct program instantiation and _main call with argument conversion.
   * Converts command-line String arguments to EK9 types and calls _main.
   */
  private void generateDirectProgramCall(final MethodVisitor mv, final ProgramDetails program,
                                         final int argsOffset) {
    final var parameterSignature = program.parameterSignature();

    // Special handling for List of String parameter (guaranteed to be the only parameter)
    if (parameterSignature.size() == 1
        && EK9_LIST_OF_STRING.equals(
        parameterSignature.getFirst().type())) {
      generateListOfStringProgramCall(mv, program, argsOffset);
      return;
    }

    // Validate argument count matches parameter count
    // if (args.length - argsOffset != parameterSignature.size()) { show error and return }
    Label argumentCountValid = new Label();

    mv.visitVarInsn(ALOAD, 0); // Load args
    mv.visitInsn(ARRAYLENGTH);
    mv.visitLdcInsn(argsOffset);
    mv.visitInsn(ISUB); // args.length - argsOffset
    mv.visitLdcInsn(parameterSignature.size());
    mv.visitJumpInsn(IF_ICMPEQ, argumentCountValid);

    // Argument count mismatch - show error
    mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
    mv.visitTypeInsn(NEW, JAVA_LANG_STRING_BUILDER);
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Error: Program '");
    mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_STRING_BUILDER, METHOD_INIT, DESC_STRING_TO_VOID,
        false);
    mv.visitLdcInsn(program.qualifiedName());
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitLdcInsn("' requires ");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitLdcInsn(parameterSignature.size());
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_INT_TO_STRING_BUILDER, false);
    mv.visitLdcInsn(" argument(s), but ");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ARRAYLENGTH);
    mv.visitLdcInsn(argsOffset);
    mv.visitInsn(ISUB);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_INT_TO_STRING_BUILDER, false);
    mv.visitLdcInsn(" provided");
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append",
        DESC_STRING_TO_STRING_BUILDER, false);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "toString", DESC_VOID_TO_STRING,
        false);
    mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
    mv.visitLdcInsn(PROGRAM_ARGUMENT_COUNT_MISMATCH_EXIT_CODE);
    mv.visitMethodInsn(INVOKESTATIC, JAVA_LANG_SYSTEM, "exit", "(I)V", false);

    // Label: argumentCountValid
    mv.visitLabel(argumentCountValid);

    // Create instance of the EK9 program class
    final var javaClassName = convertToJavaClassName(program.qualifiedName());
    final var internalClassName = javaClassName.replace(".", "/");

    // new ProgramClass()
    mv.visitTypeInsn(NEW, internalClassName);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, internalClassName, METHOD_INIT, DESC_VOID_TO_VOID, false);

    // Store instance in local variable (after converting parameters)
    final int instanceLocalVar = 1 + parameterSignature.size();
    mv.visitVarInsn(ASTORE, instanceLocalVar);

    // Convert each command-line argument to EK9 type and validate conversion succeeded
    for (int i = 0; i < parameterSignature.size(); i++) {
      final var param = parameterSignature.get(i);
      final int paramLocalVar = 1 + i; // Start at local var 1 (0 is args array)

      // Load command-line argument: args[argsOffset + i]
      mv.visitVarInsn(ALOAD, 0); // Load args array
      mv.visitLdcInsn(argsOffset + i); // Index
      mv.visitInsn(AALOAD); // args[index] - Java String

      // Store original string argument for error message
      mv.visitInsn(DUP); // Duplicate string for potential error message
      final int stringArgLocalVar = instanceLocalVar + 1 + i;
      mv.visitVarInsn(ASTORE, stringArgLocalVar);

      // Convert to EK9 type using factory method
      generateTypeConversion(mv, param.type());

      // Store converted EK9 object in local variable
      mv.visitVarInsn(ASTORE, paramLocalVar);

      // Validate conversion succeeded: if (!convertedValue._isSet()._true()) { error }
      Label conversionSuccessful = new Label();

      mv.visitVarInsn(ALOAD, paramLocalVar); // Load converted EK9 object
      mv.visitTypeInsn(CHECKCAST, EK9_LANG_BUILTIN_TYPE); // Cast to BuiltinType
      mv.visitMethodInsn(INVOKEVIRTUAL, EK9_LANG_BUILTIN_TYPE, "_isSet", DESC_VOID_TO_EK9_BOOLEAN, false);
      mv.visitMethodInsn(INVOKEVIRTUAL, EK9_LANG_BOOLEAN, "_true", DESC_VOID_TO_BOOLEAN_PRIMITIVE, false);
      mv.visitJumpInsn(IFNE, conversionSuccessful); // If true, conversion succeeded

      // Conversion failed - show error message
      mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
      mv.visitTypeInsn(NEW, JAVA_LANG_STRING_BUILDER);
      mv.visitInsn(DUP);
      mv.visitLdcInsn("Error: Invalid argument for parameter ");
      mv.visitMethodInsn(INVOKESPECIAL, JAVA_LANG_STRING_BUILDER, METHOD_INIT, DESC_STRING_TO_VOID, false);
      mv.visitLdcInsn(i + 1); // Parameter number (1-based)
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_INT_TO_STRING_BUILDER, false);
      mv.visitLdcInsn(" (");
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_STRING_TO_STRING_BUILDER, false);
      mv.visitLdcInsn(param.name());
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_STRING_TO_STRING_BUILDER, false);
      mv.visitLdcInsn("): cannot convert '");
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_STRING_TO_STRING_BUILDER, false);
      mv.visitVarInsn(ALOAD, stringArgLocalVar); // Load original string
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_STRING_TO_STRING_BUILDER, false);
      mv.visitLdcInsn("' to ");
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_STRING_TO_STRING_BUILDER, false);
      mv.visitLdcInsn(getSimpleTypeName(param.type()));
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "append", DESC_STRING_TO_STRING_BUILDER, false);
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_LANG_STRING_BUILDER, "toString", DESC_VOID_TO_STRING, false);
      mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID, false);
      mv.visitLdcInsn(PROGRAM_ARGUMENT_TYPE_MISMATCH_EXIT_CODE);
      mv.visitMethodInsn(INVOKESTATIC, JAVA_LANG_SYSTEM, "exit", "(I)V", false);

      mv.visitLabel(conversionSuccessful);
    }

    // Load instance and call _main with converted parameters
    mv.visitVarInsn(ALOAD, instanceLocalVar); // Load instance

    // Load each converted parameter
    for (int i = 0; i < parameterSignature.size(); i++) {
      mv.visitVarInsn(ALOAD, 1 + i); // Load converted parameter
    }

    // Build _main method descriptor and call
    final var mainMethodDescriptor = buildMainMethodDescriptor(parameterSignature);
    mv.visitMethodInsn(INVOKEVIRTUAL, internalClassName, METHOD_MAIN, mainMethodDescriptor, false);
  }

  /**
   * Generate bytecode for program with List of String parameter.
   * Collects all remaining command-line args into List and converts to EK9 List of String.
   */
  private void generateListOfStringProgramCall(final MethodVisitor mv, final ProgramDetails program,
                                               final int argsOffset) {
    final var javaClassName = convertToJavaClassName(program.qualifiedName());
    final var internalClassName = javaClassName.replace(".", "/");

    // Create program instance
    mv.visitTypeInsn(NEW, internalClassName);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, internalClassName, METHOD_INIT, DESC_VOID_TO_VOID, false);
    final int instanceLocalVar = 1;
    mv.visitVarInsn(ASTORE, instanceLocalVar);

    // Create ArrayList to collect all arguments
    mv.visitTypeInsn(NEW, JAVA_UTIL_ARRAY_LIST);
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, JAVA_UTIL_ARRAY_LIST, METHOD_INIT, DESC_VOID_TO_VOID, false);
    final int listLocalVar = 2;
    mv.visitVarInsn(ASTORE, listLocalVar);

    // Calculate loop bounds: from argsOffset to args.length - 1
    final int loopIndexVar = 3;
    final int argsLengthVar = 4;

    // Store args.length in local variable
    mv.visitVarInsn(ALOAD, 0); // Load args[]
    mv.visitInsn(ARRAYLENGTH);
    mv.visitVarInsn(ISTORE, argsLengthVar);

    // Initialize loop counter: i = argsOffset
    mv.visitLdcInsn(argsOffset);
    mv.visitVarInsn(ISTORE, loopIndexVar);

    // Loop: while (i < args.length)
    Label loopStart = new Label();
    Label loopEnd = new Label();

    mv.visitLabel(loopStart);
    // Check condition: i < args.length
    mv.visitVarInsn(ILOAD, loopIndexVar);
    mv.visitVarInsn(ILOAD, argsLengthVar);
    mv.visitJumpInsn(IF_ICMPGE, loopEnd); // Jump to end if i >= args.length

    // Convert args[i] to EK9 String and add to ArrayList
    mv.visitVarInsn(ALOAD, 0); // Load args[]
    mv.visitVarInsn(ILOAD, loopIndexVar); // Load i
    mv.visitInsn(AALOAD); // args[i] - Java String

    // Convert to EK9 String: String._of(javaString)
    mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_STRING, "_of",
        DESC_STRING_TO_EK9_STRING, false);

    // Add to ArrayList: list.add(ek9String)
    mv.visitVarInsn(ALOAD, listLocalVar); // Load ArrayList
    mv.visitInsn(SWAP); // Swap so list is on top, ek9String is second
    mv.visitMethodInsn(INVOKEINTERFACE, JAVA_UTIL_LIST, "add",
        PARAM_OBJECT + "Z", true);
    mv.visitInsn(POP); // Discard boolean return value

    // Increment loop counter: i++
    mv.visitIincInsn(loopIndexVar, 1);
    mv.visitJumpInsn(GOTO, loopStart);

    mv.visitLabel(loopEnd);

    // Convert List to EK9 List of String using factory method
    // _List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1._of(List)
    mv.visitVarInsn(ALOAD, listLocalVar);
    mv.visitMethodInsn(INVOKESTATIC,
        "org/ek9/lang/_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1",
        "_of",
        PARAM_UTIL_LIST + "Lorg/ek9/lang/_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1;",
        false);
    final int ek9ListLocalVar = 5;
    mv.visitVarInsn(ASTORE, ek9ListLocalVar);

    // Call program._main(ek9ListOfString)
    mv.visitVarInsn(ALOAD, instanceLocalVar); // Load program instance
    mv.visitVarInsn(ALOAD, ek9ListLocalVar); // Load EK9 List of String

    // Build method descriptor using descriptorConverter: (Lorg/ek9/lang/_List_...;)V
    final String listDescriptor = descriptorConverter.apply(
        EK9_LIST_OF_STRING);
    final String methodDescriptor = "(" + listDescriptor + ")V";
    mv.visitMethodInsn(INVOKEVIRTUAL, internalClassName, METHOD_MAIN, methodDescriptor, false);
  }

  /**
   * Generate bytecode to convert a Java String to an EK9 type using factory method.
   * Stack: [javaString] -> [ek9Object]
   * Supports all types defined in ProgramArgumentPredicate.
   * Note: Caller must validate conversion succeeded by checking _isSet() on returned object.
   */
  private void generateTypeConversion(final MethodVisitor mv, final String ek9Type) {
    // Call appropriate EK9 factory method: Type._of(javaString)
    // All these types support _of(String) factory method for command-line conversion
    // The _of method returns an unset object if conversion fails
    switch (ek9Type) {
      // Basic types
      case EK9_STRING -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_STRING, "_of",
          DESC_STRING_TO_EK9_STRING, false);
      case EK9_INTEGER -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_INTEGER, "_of",
          DESC_STRING_TO_EK9_INTEGER, false);
      case EK9_FLOAT -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_FLOAT, "_of",
          DESC_STRING_TO_EK9_FLOAT, false);
      case EK9_BOOLEAN -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_BOOLEAN, "_of",
          DESC_STRING_TO_EK9_BOOLEAN, false);
      case EK9_CHARACTER -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_CHARACTER,
          "_of", DESC_STRING_TO_EK9_CHARACTER, false);
      case EK9_BITS -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_BITS, "_of",
          DESC_STRING_TO_EK9_BITS, false);

      // Date/Time types
      case EK9_DATE -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_DATE, "_of",
          DESC_STRING_TO_EK9_DATE, false);
      case EK9_DATETIME -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_DATETIME,
          "_of", DESC_STRING_TO_EK9_DATETIME, false);
      case EK9_TIME -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_TIME, "_of",
          DESC_STRING_TO_EK9_TIME, false);
      case EK9_DURATION -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_DURATION,
          "_of", DESC_STRING_TO_EK9_DURATION, false);
      case EK9_MILLISECOND -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_MILLISECOND,
          "_of", DESC_STRING_TO_EK9_MILLISECOND, false);

      // Physical/Visual types
      case EK9_DIMENSION -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_DIMENSION,
          "_of", DESC_STRING_TO_EK9_DIMENSION, false);
      case EK9_RESOLUTION -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_RESOLUTION,
          "_of", DESC_STRING_TO_EK9_RESOLUTION, false);
      case EK9_COLOUR -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_COLOUR, "_of",
          DESC_STRING_TO_EK9_COLOUR, false);

      // Financial/Pattern types
      case EK9_MONEY -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_MONEY, "_of",
          DESC_STRING_TO_EK9_MONEY, false);
      case EK9_REGEX -> mv.visitMethodInsn(INVOKESTATIC, EK9_LANG_REGEX, "_of",
          DESC_STRING_TO_EK9_REGEX, false);

      default -> {
        // Unsupported type - show error and exit
        mv.visitInsn(POP); // Remove javaString from stack
        mv.visitFieldInsn(GETSTATIC, JAVA_LANG_SYSTEM, "err", DESC_PRINT_STREAM);
        mv.visitLdcInsn("Error: Unsupported parameter type: " + ek9Type);
        mv.visitMethodInsn(INVOKEVIRTUAL, JAVA_IO_PRINT_STREAM, "println", DESC_STRING_TO_VOID,
            false);
        mv.visitInsn(RETURN);
      }
    }
  }
}