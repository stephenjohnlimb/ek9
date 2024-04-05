package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;

/**
 * Typically used in control expressions to check developer is not doing a while(true).
 * But also used in other controls.
 */
final class CheckNotABooleanLiteral implements Consumer<EK9Parser.ExpressionContext> {
  private final ErrorListener errorListener;

  CheckNotABooleanLiteral(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ExpressionContext ctx) {

    if (ctx != null
        && ctx.primary() != null
        && ctx.primary().literal() instanceof EK9Parser.BooleanLiteralContext) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.POINTLESS_EXPRESSION);
    }

  }
}
