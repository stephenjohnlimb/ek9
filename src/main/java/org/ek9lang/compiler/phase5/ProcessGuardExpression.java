package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.UninitialisedVariableToBeChecked;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;

/**
 * Checks on a guard expression and marks the identifier variable as initialised.
 */
final class ProcessGuardExpression extends TypedSymbolAccess implements Consumer<EK9Parser.GuardExpressionContext> {
  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  ProcessGuardExpression(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.GuardExpressionContext ctx) {

    var symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.identifier());
    if (uninitialisedVariableToBeChecked.test(symbol)) {
      symbolAndScopeManagement.recordSymbolAssignment(symbol);
    }

  }
}
