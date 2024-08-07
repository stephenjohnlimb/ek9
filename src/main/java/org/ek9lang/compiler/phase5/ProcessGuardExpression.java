package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.common.UninitialisedVariableToBeChecked;

/**
 * Checks on a guard expression and marks the identifier variable as initialised.
 */
final class ProcessGuardExpression extends TypedSymbolAccess implements Consumer<EK9Parser.GuardExpressionContext> {
  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  ProcessGuardExpression(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.GuardExpressionContext ctx) {

    final var symbol = symbolsAndScopes.getRecordedSymbol(ctx.identifier());
    if (uninitialisedVariableToBeChecked.test(symbol)) {
      symbolsAndScopes.recordSymbolAssignment(symbol);
    }

  }
}
