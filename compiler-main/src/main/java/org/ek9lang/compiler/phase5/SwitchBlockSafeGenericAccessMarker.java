package org.ek9lang.compiler.phase5;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

final class SwitchBlockSafeGenericAccessMarker extends AbstractSafeGenericAccessMarker
    implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  SwitchBlockSafeGenericAccessMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {
    final var preFlowCtx = ctx.preFlowAndControl().preFlowStatement();

    if (preFlowCtx != null) {
      //This is the context that would be safe if the switch pre-flow was used with a variable
      //That would effectively be 'made safe' in the whole switch scope.
      final var wouldBeSafeScope = symbolsAndScopes.getRecordedScope(ctx);
      processPreFlow(preFlowCtx, wouldBeSafeScope);
    }
  }
}
