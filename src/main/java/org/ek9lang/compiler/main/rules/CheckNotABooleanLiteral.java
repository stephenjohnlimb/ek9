package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

public class CheckNotABooleanLiteral implements Consumer<EK9Parser.ExpressionContext> {
  private final ErrorListener errorListener;

  public CheckNotABooleanLiteral(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.ExpressionContext ctx) {
    if (ctx != null
        && ctx.primary() != null
        && ctx.primary().literal() instanceof EK9Parser.BooleanLiteralContext) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.POINTLESS_EXPRESSION);
    }
  }
}
