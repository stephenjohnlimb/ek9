package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.LiteralInstr;
import org.ek9lang.compiler.phase7.generation.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;

/**
 * Deals with generating the correct IR Instructions for processing any form of literal.
 * Memory management is handled by the higher-level expression processor.
 * 
 * MIGRATED: Now uses IRInstructionBuilder with original IRContext access via stack infrastructure.
 */
final class LiteralGenerator implements Function<LiteralProcessingDetails, List<IRInstr>> {

  private final IRInstructionBuilder instructionBuilder;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();

  LiteralGenerator(final IRInstructionBuilder instructionBuilder) {
    this.instructionBuilder = instructionBuilder;
  }

  @Override
  public List<IRInstr> apply(final LiteralProcessingDetails details) {
    // Get the type from the resolved symbol (could be decorated for generic contexts)
    final var literalType = typeNameOrException.apply(details.literalSymbol());
    final var literalValue = variableNameForIR.apply(details.literalSymbol());

    final var debugInfo = instructionBuilder.createDebugInfo(details.literalSymbol().getSourceToken());
    // Create literal instruction with original debug info (correct position)
    // Don't apply memory management here - let the higher-level expression processor handle it
    return List.of(LiteralInstr.literal(details.literalResult(), literalValue, literalType, debugInfo));
  }
}
