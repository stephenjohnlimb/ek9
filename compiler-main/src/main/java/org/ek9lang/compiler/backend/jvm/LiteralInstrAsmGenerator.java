package org.ek9lang.compiler.backend.jvm;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

/**
 * Specialized ASM generator for LiteralInstr processing.
 * Handles loading of literal/constant values into JVM bytecode
 * using the actual LiteralInstr typed methods (no string parsing).
 */
public final class LiteralInstrAsmGenerator extends AbstractAsmGenerator {

  public LiteralInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                                  final OutputVisitor outputVisitor,
                                  final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a literal loading instruction.
   * Uses LiteralInstr typed methods to extract literal value and type.
   */
  public void generateLiteral(final LiteralInstr literalInstr) {
    AssertValue.checkNotNull("LiteralInstr cannot be null", literalInstr);

    // Generate debug info if available
    literalInstr.getDebugInfo().ifPresent(this::generateDebugInfo);

    // Extract literal details using typed methods (not string parsing)
    final var literalValue = literalInstr.getLiteralValue();
    final var literalType = literalInstr.getLiteralType();

    // Generate appropriate JVM instruction based on literal type
    generateLiteralByType(literalValue, literalType);

    // Store result if instruction has one and track temp variable source
    if (literalInstr.hasResult()) {
      final var resultVar = literalInstr.getResult();
      trackTempVariableFromLiteral(resultVar, literalValue, literalType);
      generateStoreVariable(resultVar);
    }
  }

  /**
   * Generate JVM bytecode for loading literal based on its EK9 type.
   */
  private void generateLiteralByType(final String literalValue, final String literalType) {
    switch (literalType) {
      case "org.ek9.lang::String" -> generateStringLiteral(literalValue);
      case "org.ek9.lang::Integer" -> generateIntegerLiteral(literalValue);
      case "org.ek9.lang::Boolean" -> generateBooleanLiteral(literalValue);
      case "org.ek9.lang::Float" -> generateFloatLiteral(literalValue);
      case "org.ek9.lang::Character" -> generateCharacterLiteral(literalValue);
      default -> generateObjectLiteral(literalValue, literalType);
    }
  }

  /**
   * Generate JVM bytecode for string literal.
   * Creates EK9 String object from Java string literal using actual IR type.
   */
  private void generateStringLiteral(final String value) {
    // Load the string literal onto stack
    getCurrentMethodVisitor().visitLdcInsn(value);

    // Use proper IR type conversion for EK9 String
    final var jvmStringType = convertToJvmName("org.ek9.lang::String");
    final var stringDescriptor = convertToJvmDescriptor("org.ek9.lang::String");

    // Create new EK9 String object from Java string using _of method
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmStringType,
        "_of",
        "(Ljava/lang/String;)" + stringDescriptor,
        false
    );
  }

  /**
   * Generate JVM bytecode for integer literal.
   */
  private void generateIntegerLiteral(final String value) {
    try {
      final int intValue = Integer.parseInt(value);

      // Use appropriate constant loading instruction based on value
      switch (intValue) {
        case -1 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_M1);
        case 0 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_0);
        case 1 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_1);
        case 2 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_2);
        case 3 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_3);
        case 4 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_4);
        case 5 -> getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_5);
        default -> {
          if (intValue >= Byte.MIN_VALUE && intValue <= Byte.MAX_VALUE) {
            getCurrentMethodVisitor().visitIntInsn(Opcodes.BIPUSH, intValue);
          } else if (intValue >= Short.MIN_VALUE && intValue <= Short.MAX_VALUE) {
            getCurrentMethodVisitor().visitIntInsn(Opcodes.SIPUSH, intValue);
          } else {
            getCurrentMethodVisitor().visitLdcInsn(intValue);
          }
        }
      }

      // Create EK9 Integer object from primitive int using actual IR type
      final var jvmIntegerType = convertToJvmName("org.ek9.lang::Integer");
      final var integerDescriptor = convertToJvmDescriptor("org.ek9.lang::Integer");

      // Convert int to String first, then use _of method
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "java/lang/Integer",
          "toString",
          "(I)Ljava/lang/String;",
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          jvmIntegerType,
          "_of",
          "(Ljava/lang/String;)" + integerDescriptor,
          false
      );

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid integer literal: " + value, e);
    }
  }

  /**
   * Generate JVM bytecode for boolean literal.
   */
  private void generateBooleanLiteral(final String value) {
    // Load boolean value as int (0 or 1)
    if ("true".equals(value)) {
      getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_1);
    } else if ("false".equals(value)) {
      getCurrentMethodVisitor().visitInsn(Opcodes.ICONST_0);
    } else {
      throw new IllegalArgumentException("Invalid boolean literal: " + value);
    }

    // Create EK9 Boolean object from primitive boolean using actual IR type
    final var jvmBooleanType = convertToJvmName("org.ek9.lang::Boolean");
    final var booleanDescriptor = convertToJvmDescriptor("org.ek9.lang::Boolean");

    // Convert boolean to String first, then use _of method
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        "java/lang/Boolean",
        "toString",
        "(Z)Ljava/lang/String;",
        false
    );

    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmBooleanType,
        "_of",
        "(Ljava/lang/String;)" + booleanDescriptor,
        false
    );
  }

  /**
   * Generate JVM bytecode for float literal.
   */
  private void generateFloatLiteral(final String value) {
    try {
      final float floatValue = Float.parseFloat(value);

      // Use appropriate constant loading instruction
      if (floatValue == 0.0f) {
        getCurrentMethodVisitor().visitInsn(Opcodes.FCONST_0);
      } else if (floatValue == 1.0f) {
        getCurrentMethodVisitor().visitInsn(Opcodes.FCONST_1);
      } else if (floatValue == 2.0f) {
        getCurrentMethodVisitor().visitInsn(Opcodes.FCONST_2);
      } else {
        getCurrentMethodVisitor().visitLdcInsn(floatValue);
      }

      // Create EK9 Float object from primitive float using actual IR type
      final var jvmFloatType = convertToJvmName("org.ek9.lang::Float");
      final var floatDescriptor = convertToJvmDescriptor("org.ek9.lang::Float");

      // Convert float to String first, then use _of method
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "java/lang/Float",
          "toString",
          "(F)Ljava/lang/String;",
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          jvmFloatType,
          "_of",
          "(Ljava/lang/String;)" + floatDescriptor,
          false
      );

    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid float literal: " + value, e);
    }
  }

  /**
   * Generate JVM bytecode for character literal.
   */
  private void generateCharacterLiteral(final String value) {
    if (value.length() == 1) {
      final char charValue = value.charAt(0);
      getCurrentMethodVisitor().visitIntInsn(Opcodes.BIPUSH, charValue);

      // Create EK9 Character object from primitive char using actual IR type
      final var jvmCharacterType = convertToJvmName("org.ek9.lang::Character");
      final var characterDescriptor = convertToJvmDescriptor("org.ek9.lang::Character");

      // Convert char to String first, then use _of method
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          "java/lang/Character",
          "toString",
          "(C)Ljava/lang/String;",
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          jvmCharacterType,
          "_of",
          "(Ljava/lang/String;)" + characterDescriptor,
          false
      );
    } else {
      throw new IllegalArgumentException("Invalid character literal: " + value);
    }
  }

  /**
   * Generate JVM bytecode for generic object literal.
   * This handles complex literals that don't fit standard primitive categories.
   */
  private void generateObjectLiteral(final String value, final String type) {
    // For now, treat as string and let the target type constructor handle parsing
    getCurrentMethodVisitor().visitLdcInsn(value);

    // Call constructor of the target type
    final var jvmTypeName = convertToJvmName(type);
    final var typeDescriptor = convertToJvmDescriptor(type);
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmTypeName,
        "_of",
        "(Ljava/lang/String;)" + typeDescriptor,
        false
    );
  }
}