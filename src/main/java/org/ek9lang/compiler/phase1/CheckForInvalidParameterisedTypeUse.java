package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Error when use of ParameterisedType has additional expression parameters in wrong context.
 */
final class CheckForInvalidParameterisedTypeUse implements Consumer<EK9Parser.ParameterisedTypeContext> {
  private final ErrorListener errorListener;

  CheckForInvalidParameterisedTypeUse(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.ParameterisedTypeContext ctx) {
    if (ctx.paramExpression() != null && !(ctx.getParent() instanceof EK9Parser.CallContext)) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.PARENTHESIS_NOT_REQUIRED);
    } else if (ctx.paramExpression() == null && ctx.getParent() instanceof EK9Parser.CallContext) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.PARENTHESIS_REQUIRED);
    } else if (ctx.paramExpression() != null && ctx.getParent() instanceof EK9Parser.CallContext
        && !ctx.paramExpression().expressionParam().isEmpty()) {
      errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.VALUES_AND_TYPE_INCOMPATIBLE);
    }
  }
}
