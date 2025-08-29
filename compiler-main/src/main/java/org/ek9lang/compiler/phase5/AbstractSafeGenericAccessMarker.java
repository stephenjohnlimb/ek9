package org.ek9lang.compiler.phase5;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.LhsFromPreFlowOrError;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Does checks on the control part of a while loop to see if Optional/Result/Iterator are now safe via method access.
 */
abstract class AbstractSafeGenericAccessMarker {

  protected final SymbolsAndScopes symbolsAndScopes;
  private final ExpressionSimpleForSafeAccess expressionSimpleForSafeAccess = new ExpressionSimpleForSafeAccess();
  private final ExpressionSafeSymbolMarker expressionSafeSymbolMarker;
  private final SafeSymbolMarker safeSymbolMarker;
  private final LhsFromPreFlowOrError lhsFromPreFlowOrError;

  /**
   * Constructor to provided typed access.
   */
  AbstractSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.expressionSafeSymbolMarker = new ExpressionSafeSymbolMarker(symbolsAndScopes, errorListener);
    this.safeSymbolMarker = new SafeSymbolMarker(symbolsAndScopes, errorListener);
    this.lhsFromPreFlowOrError = new LhsFromPreFlowOrError(symbolsAndScopes, errorListener);
  }

  protected void processPreFlow(final EK9Parser.PreFlowStatementContext ctx,
                                final IScope wouldBeSafeScope) {
    //This is the context that would be safe if the switch pre-flow was used with a variable
    final var preFlowVariable = lhsFromPreFlowOrError.apply(ctx);
    safeSymbolMarker.accept(preFlowVariable, wouldBeSafeScope);
  }

  protected void processExpression(final EK9Parser.ExpressionContext ctx,
                                   final IScope wouldBeSafeScope) {
    if (expressionSimpleForSafeAccess.test(ctx)) {
      expressionSafeSymbolMarker.accept(ctx, wouldBeSafeScope);
    }
  }
}
