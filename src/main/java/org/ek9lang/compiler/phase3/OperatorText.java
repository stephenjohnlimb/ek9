package org.ek9lang.compiler.phase3;

import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;

/**
 * Returns the appropriate operator text symbol for the expression.
 * This assumes there is an operator.
 */
final class OperatorText implements Function<EK9Parser.ExpressionContext, String> {
  @Override
  public String apply(final EK9Parser.ExpressionContext ctx) {

    String searchMethodName = null;
    if (ctx != null && ctx.op != null) {
      searchMethodName = ctx.op.getText();
      if (searchMethodName.equals("not")) {
        return "~";
      } else if (searchMethodName.equals("!=")) {
        return "<>";
      }
    }
    return searchMethodName;
  }
}
