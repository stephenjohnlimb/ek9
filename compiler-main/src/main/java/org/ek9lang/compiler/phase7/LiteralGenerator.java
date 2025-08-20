package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.LiteralInstr;
import org.ek9lang.compiler.phase7.support.DebugInfoCreator;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.LiteralProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;

/**
 * Deals with generating the correct IR Instructions for processing any form of literal.
 * This includes the memory management.
 */
final class LiteralGenerator implements Function<LiteralProcessingDetails, List<IRInstr>> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableNameForIR variableNameForIR = new VariableNameForIR();
  private final DebugInfoCreator debugInfoCreator;

  LiteralGenerator(final IRContext context) {
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  @Override
  public List<IRInstr> apply(final LiteralProcessingDetails details) {
    final var instructions = new ArrayList<IRInstr>();

    // Get the type from the resolved symbol (could be decorated for generic contexts)
    final var literalType = typeNameOrException.apply(details.literalSymbol());
    final var literalValue = variableNameForIR.apply(details.literalSymbol());

    // Extract debug info if debugging instrumentation is enabled
    final var debugInfo = debugInfoCreator.apply(details.literalSymbol().getSourceToken());

    // Create literal instruction with resolved type information
    instructions.add(LiteralInstr.literal(details.literalResult(), literalValue, literalType, debugInfo));

    return instructions;
  }
}
