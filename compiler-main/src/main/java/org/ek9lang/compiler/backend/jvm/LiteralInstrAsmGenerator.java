package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_BOOLEAN_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_CHAR_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_FLOAT_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.DESC_INT_TO_STRING;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_BOOLEAN;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_CHARACTER;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_FLOAT;
import static org.ek9lang.compiler.support.JVMTypeNames.JAVA_LANG_INTEGER;
import static org.ek9lang.compiler.support.JVMTypeNames.PARAM_STRING;

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
      case EK9_STRING -> generateStringLiteral(literalValue);
      case EK9_INTEGER -> generateIntegerLiteral(literalValue);
      case EK9_BOOLEAN -> generateBooleanLiteral(literalValue);
      case EK9_FLOAT -> generateFloatLiteral(literalValue);
      case EK9_CHARACTER -> generateCharacterLiteral(literalValue);
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
    final var jvmStringType = convertToJvmName(EK9_STRING);
    final var stringDescriptor = convertToJvmDescriptor(EK9_STRING);

    // Create new EK9 String object from Java string using _of method
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmStringType,
        "_of",
        PARAM_STRING + stringDescriptor,
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
      final var jvmIntegerType = convertToJvmName(EK9_INTEGER);
      final var integerDescriptor = convertToJvmDescriptor(EK9_INTEGER);

      // Convert int to String first, then use _of method
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          JAVA_LANG_INTEGER,
          "toString",
          DESC_INT_TO_STRING,
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          jvmIntegerType,
          "_of",
          PARAM_STRING + integerDescriptor,
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
    final var jvmBooleanType = convertToJvmName(EK9_BOOLEAN);
    final var booleanDescriptor = convertToJvmDescriptor(EK9_BOOLEAN);

    // Convert boolean to String first, then use _of method
    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        JAVA_LANG_BOOLEAN,
        "toString",
        DESC_BOOLEAN_TO_STRING,
        false
    );

    getCurrentMethodVisitor().visitMethodInsn(
        Opcodes.INVOKESTATIC,
        jvmBooleanType,
        "_of",
        PARAM_STRING + booleanDescriptor,
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
      final var jvmFloatType = convertToJvmName(EK9_FLOAT);
      final var floatDescriptor = convertToJvmDescriptor(EK9_FLOAT);

      // Convert float to String first, then use _of method
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          JAVA_LANG_FLOAT,
          "toString",
          DESC_FLOAT_TO_STRING,
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          jvmFloatType,
          "_of",
          PARAM_STRING + floatDescriptor,
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
      final var jvmCharacterType = convertToJvmName(EK9_CHARACTER);
      final var characterDescriptor = convertToJvmDescriptor(EK9_CHARACTER);

      // Convert char to String first, then use _of method
      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          JAVA_LANG_CHARACTER,
          "toString",
          DESC_CHAR_TO_STRING,
          false
      );

      getCurrentMethodVisitor().visitMethodInsn(
          Opcodes.INVOKESTATIC,
          jvmCharacterType,
          "_of",
          PARAM_STRING + characterDescriptor,
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
        PARAM_STRING + typeDescriptor,
        false
    );
  }
}