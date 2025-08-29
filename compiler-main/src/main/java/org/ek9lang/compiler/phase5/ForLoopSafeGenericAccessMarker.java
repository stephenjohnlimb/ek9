package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Does checks on both for loops and for range in terms of pre-flow and expressions (for loop).
 * Makes access safe as appropriate for specific types like Optional and Result.
 */
final class ForLoopSafeGenericAccessMarker extends AbstractSafeGenericAccessMarker
    implements Consumer<EK9Parser.ForStatementExpressionContext> {

  /**
   * Constructor to provided typed access.
   */
  ForLoopSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  /**
   * Assess the control expression to see if it makes the necessary calls to make appropriate generic object
   * methods safe.
   */
  @Override
  public void accept(final EK9Parser.ForStatementExpressionContext ctx) {

    //Now if there is a preflow part - we can make that variable safe within the loop.
    final var preFlowCtx = ctx.forLoop() != null ? ctx.forLoop().preFlowStatement() : ctx.forRange().preFlowStatement();

    //That would effectively be 'made safe' in the whole for scope.
    final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx);

    if (preFlowCtx != null) {
      processPreFlow(preFlowCtx, wouldBeSafeScope);
    }

    //Only check this on a while, not do/while. Think about it the block can only be safe if check is done in while.
    if (ctx.forLoop() != null) {
      final var expressionCtx = ctx.forLoop().expression();
      processExpression(expressionCtx, wouldBeSafeScope);
    }
  }
}
