package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Checks that either all parameters are named when making calls, or none are named at all.
 * This is to drive consistency.
 */
public class CheckParamExpressionNamedParameters implements Consumer<EK9Parser.ParamExpressionContext> {
  private final ErrorListener errorListener;

  public CheckParamExpressionNamedParameters(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  /**
   * Check either all params are named or non are named.
   */
  @Override
  public void accept(final EK9Parser.ParamExpressionContext ctx) {
    var numParams = ctx.expressionParam().size();
    var numNamedParams = ctx.expressionParam().stream().filter(param -> param.identifier() != null).count();

    if (numNamedParams != 0 && numNamedParams != numParams) {
      //So there are some named and other not named.
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.EITHER_ALL_PARAMETERS_NAMED_OR_NONE);
    }
  }
}
