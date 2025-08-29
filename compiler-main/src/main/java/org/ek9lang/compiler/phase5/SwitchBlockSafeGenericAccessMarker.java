package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.LhsFromPreFlowOrError;
import org.ek9lang.compiler.common.SymbolsAndScopes;

final class SwitchBlockSafeGenericAccessMarker implements Consumer<EK9Parser.SwitchStatementExpressionContext> {
  private final SymbolsAndScopes symbolsAndScopes;
  private final SafeSymbolMarker safeSymbolMarker;
  private final LhsFromPreFlowOrError lhsFromPreFlowOrError;

  SwitchBlockSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.safeSymbolMarker = new SafeSymbolMarker(symbolsAndScopes, errorListener);
    this.lhsFromPreFlowOrError = new LhsFromPreFlowOrError(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {
    final var preFlowCtx = ctx.preFlowAndControl().preFlowStatement();

    if (preFlowCtx != null) {
      //This is the context that would be safe if the switch pre-flow was used with a variable
      //That would effectively be 'made safe' in the whole switch scope.
      final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx);
      final var preFlowVariable = lhsFromPreFlowOrError.apply(preFlowCtx);
      safeSymbolMarker.accept(preFlowVariable, wouldBeSafeScope);
    }
  }
}
