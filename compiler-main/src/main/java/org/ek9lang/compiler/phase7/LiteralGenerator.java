package org.ek9lang.compiler.phase7;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.LiteralInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRInstructionBuilder;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;

/**
 * Deals with generating the correct IR Instructions for processing any form of literal.
 * This includes the memory management.
 * 
 * MIGRATED: Now uses IRInstructionBuilder with original IRContext access via stack infrastructure.
 */
final class LiteralGenerator implements Function<LiteralProcessingDetails, List<IRInstr>> {

  private final IRInstructionBuilder instructionBuilder;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final DebugInfoCreator debugInfoCreator;

  LiteralGenerator(final IRInstructionBuilder instructionBuilder) {
    this.instructionBuilder = instructionBuilder;
    // Access original IRContext via stack infrastructure - no parameter threading!
    this.debugInfoCreator = new DebugInfoCreator(instructionBuilder.getIRContext());
  }

  @Override
  public List<IRInstr> apply(final LiteralProcessingDetails details) {
    // Get the type from the resolved symbol (could be decorated for generic contexts)
    final var literalType = typeNameOrException.apply(details.literalSymbol());
    final var literalValue = variableNameForIR.apply(details.literalSymbol());

    // Extract debug info using original IRContext from stack infrastructure
    final var debugInfo = debugInfoCreator.apply(details.literalSymbol().getSourceToken());

    // Create literal instruction with resolved type information
    return List.of(LiteralInstr.literal(details.literalResult(), literalValue, literalType, debugInfo));
  }
}
