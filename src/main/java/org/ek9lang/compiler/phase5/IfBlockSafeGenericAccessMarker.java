package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Does basic checks on the if condition to see if a Result/Optional access has been made safe.
 * This is a bit basic at present and could be made more sophisticated - but let's get something working first.
 * The idea is to mark the block safe for ok(), error() and get() calls if and only if the controlling logic
 * in the if statement guarantees that access is safe now in that block.
 * This code just uses the ANTLR AST and symbols that have been annotated.
 * It maybe that his simple check is enough for now, but before building a whole 'IR' and then analysing that
 * we should be able to 'fail early' before doing too much.
 */
final class IfBlockSafeGenericAccessMarker implements Consumer<EK9Parser.IfControlBlockContext> {
  private final ExpressionSimpleForSafeAccess expressionSimpleForSafeAccess = new ExpressionSimpleForSafeAccess();
  private final SymbolsAndScopes symbolsAndScopes;

  private final MarkAppropriateSymbolsSafe markAppropriateSymbolsSafe;

  /**
   * Constructor to provided typed access.
   */
  IfBlockSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.markAppropriateSymbolsSafe = new MarkAppropriateSymbolsSafe(symbolsAndScopes, errorListener);
  }

  /**
   * This is the expression we have to assess.
   * Now to keep this simple we only allow 'and' when working with Result/Optional and specific methods
   * that way we can keep it both simple for the compiler and also the error messages for the EK9 developer
   * So we want to support 'r1.isOk() and r2.isOK() and o1? and o2?'
   * or more idiomatically 'r1? and r2? and o1? and o2?'
   * For error conditions we also need to support 'r1.isError() and r2.isOk()' for example
   * Then r1.error() can be called and r2.ok() can also be called.
   */
  @Override
  public void accept(final EK9Parser.IfControlBlockContext ctx) {

    final var expressionCtx = ctx.preFlowAndControl().control;
    if (expressionSimpleForSafeAccess.test(expressionCtx)) {
      //This is the context that would be safe if the 'if statement' had the check in.
      final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx.block());
      markAppropriateSymbolsSafe.accept(expressionCtx, wouldBeSafeScope);
    }

  }

}
