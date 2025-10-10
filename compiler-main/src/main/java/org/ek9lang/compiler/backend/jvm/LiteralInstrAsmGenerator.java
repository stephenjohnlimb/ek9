package org.ek9lang.compiler.backend.jvm;

import static org.ek9lang.compiler.support.EK9TypeNames.EK9_BOOLEAN;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_CHARACTER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_FLOAT;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_INTEGER;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_STRING;

import java.util.function.Consumer;
import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.core.AssertValue;
import org.objectweb.asm.ClassWriter;

/**
 * Specialized ASM generator for LiteralInstr processing.
 * Handles loading of literal/constant values into JVM bytecode
 * using the actual LiteralInstr typed methods (no string parsing).
 */
final class LiteralInstrAsmGenerator extends AbstractAsmGenerator
    implements Consumer<LiteralInstr> {

  LiteralInstrAsmGenerator(final ConstructTargetTuple constructTargetTuple,
                           final OutputVisitor outputVisitor,
                           final ClassWriter classWriter) {
    super(constructTargetTuple, outputVisitor, classWriter);
  }

  /**
   * Generate JVM bytecode for a literal loading instruction.
   * Uses LiteralInstr typed methods to extract literal value and type.
   */
  @Override
  public void accept(final LiteralInstr literalInstr) {
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
   * Delegates to protected methods in AbstractAsmGenerator for actual bytecode generation.
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
}