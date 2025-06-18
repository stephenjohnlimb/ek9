package org.ek9lang.compiler.phase5;

import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;

/**
 * When looking to calculate complexity of methods/functions it is important to
 * work out if parts of an expression are in some way conditional.
 * If they are then there is a form of branching going on 0 that adds complexity.
 * Whereas just a + b (while it is an expression does not add any branching).
 */
class FormOfComparator implements Predicate<EK9Parser.ExpressionContext> {
  @Override
  public boolean test(final EK9Parser.ExpressionContext ctx) {

    if (ctx.op != null) {
      return (isASortOfComparisonOperator(ctx.op.getText()));
    }

    return ctx.coalescing != null || ctx.coalescing_equality != null || ctx.ternary != null || ctx.IN() != null;

  }

  private boolean isASortOfComparisonOperator(final String op) {

    return switch (op) {
      case "<", "<=", ">", ">=", "==", "<>", "!=", "<=>", "<~>", "matches", "contains", "?" -> true;
      default -> false;
    };

  }
}
