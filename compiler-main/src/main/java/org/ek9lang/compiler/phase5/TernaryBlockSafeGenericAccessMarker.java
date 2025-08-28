package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Similar to the if block check, but this is focused on the true part of the ternary expression.
 */
final class TernaryBlockSafeGenericAccessMarker implements Consumer<EK9Parser.ExpressionContext> {
  private final ExpressionSimpleForSafeAccess expressionSimpleForSafeAccess = new ExpressionSimpleForSafeAccess();
  private final SymbolsAndScopes symbolsAndScopes;

  private final ExpressionSafeSymbolMarker expressionSafeSymbolMarker;

  /**
   * Constructor to provided typed access.
   */
  TernaryBlockSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.expressionSafeSymbolMarker = new ExpressionSafeSymbolMarker(symbolsAndScopes, errorListener);
  }


  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {

    //So there three expressions as pat of a ternary
    //The first is the control, this might make the second expression access safe.
    final var expressionCtx = ctx.control;
    if (expressionSimpleForSafeAccess.test(expressionCtx)) {
      final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx.ternaryPart(0));
      expressionSafeSymbolMarker.accept(expressionCtx, wouldBeSafeScope);
    }

  }

}
