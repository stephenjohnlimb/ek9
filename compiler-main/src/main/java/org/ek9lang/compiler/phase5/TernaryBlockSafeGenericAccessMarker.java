package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Similar to the if block check, but this is focused on the true part of the ternary expression.
 */
final class TernaryBlockSafeGenericAccessMarker extends AbstractSafeGenericAccessMarker
    implements Consumer<EK9Parser.ExpressionContext> {

  /**
   * Constructor to provided typed access.
   */
  TernaryBlockSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }


  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {

    //So there three expressions as pat of a ternary
    //The first is the control, this might make the second expression access safe.
    final var expressionCtx = ctx.control;
    final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx.ternaryPart(0));
    processExpression(expressionCtx, wouldBeSafeScope);
  }
}
