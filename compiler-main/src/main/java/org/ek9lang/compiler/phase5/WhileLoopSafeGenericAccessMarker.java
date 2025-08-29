package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.LhsFromPreFlowOrError;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Does checks on the control part of a while loop to see if Optional/Result/Iterator are now safe via method access.
 */
final class WhileLoopSafeGenericAccessMarker implements Consumer<EK9Parser.WhileStatementExpressionContext> {
  private final ExpressionSimpleForSafeAccess expressionSimpleForSafeAccess = new ExpressionSimpleForSafeAccess();
  private final SymbolsAndScopes symbolsAndScopes;

  private final ExpressionSafeSymbolMarker expressionSafeSymbolMarker;
  private final SafeSymbolMarker safeSymbolMarker;
  private final LhsFromPreFlowOrError lhsFromPreFlowOrError;

  /**
   * Constructor to provided typed access.
   */
  WhileLoopSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.expressionSafeSymbolMarker = new ExpressionSafeSymbolMarker(symbolsAndScopes, errorListener);
    this.safeSymbolMarker = new SafeSymbolMarker(symbolsAndScopes, errorListener);
    this.lhsFromPreFlowOrError = new LhsFromPreFlowOrError(symbolsAndScopes, errorListener);
  }

  /**
   * Assess the control expression to see if it makes the necessary calls to make appropriate generic object
   * methods safe.
   */
  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    //Now if there is a preflow part for while and do/while - we can make that variable safe within the loop.
    final var preFlowCtx = ctx.preFlowStatement();

    if (preFlowCtx != null) {
      //This is the context that would be safe if the switch pre-flow was used with a variable
      //That would effectively be 'made safe' in the whole switch scope.
      final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx);
      final var preFlowVariable = lhsFromPreFlowOrError.apply(preFlowCtx);
      safeSymbolMarker.accept(preFlowVariable, wouldBeSafeScope);
    }

    //Only check this on a while, not do/while. Think about it the block can only be safe if check is done in while.
    if (ctx.WHILE() != null) {
      final var expressionCtx = ctx.control;
      if (expressionSimpleForSafeAccess.test(expressionCtx)) {
        //This is the context that would be safe if the 'while control' had the check in.
        final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx.instructionBlock());
        expressionSafeSymbolMarker.accept(expressionCtx, wouldBeSafeScope);
      }
    }

  }

}
