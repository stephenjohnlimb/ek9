package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Does checks on the control part of a while loop to see if Optional/Result/Iterator are now safe via method access.
 */
final class WhileLoopSafeGenericAccessMarker implements Consumer<EK9Parser.WhileStatementExpressionContext> {
  private final ExpressionSimpleForSafeAccess expressionSimpleForSafeAccess = new ExpressionSimpleForSafeAccess();
  private final SymbolsAndScopes symbolsAndScopes;

  private final MarkAppropriateSymbolsSafe markAppropriateSymbolsSafe;

  /**
   * Constructor to provided typed access.
   */
  WhileLoopSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.markAppropriateSymbolsSafe = new MarkAppropriateSymbolsSafe(symbolsAndScopes, errorListener);
  }

  /**
   * Assess the control expression to see if it makes the necessary calls to make appropriate generic object
   * methods safe.
   */
  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {
    //Only check this on a while, not do/while. Think about it the block can only be safe if check is done in while.
    if (ctx.WHILE() != null) {
      final var expressionCtx = ctx.control;
      if (expressionSimpleForSafeAccess.test(expressionCtx)) {
        //This is the context that would be safe if the 'while control' had the check in.
        final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx.instructionBlock());
        markAppropriateSymbolsSafe.accept(expressionCtx, wouldBeSafeScope);
      }
    }

  }

}
