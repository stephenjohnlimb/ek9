package org.ek9lang.compiler.phase5;

import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;

/**
 * Just checks if the expression as defined by the ANTLR AST is simple enough to be assessed
 * to make any Result/Optional get()/ok()/error() method calls safe.
 * This only checks the nature of the expression for using 'and' and no 'not'.
 */
final class ExpressionSimpleForSafeAccess implements Predicate<EK9Parser.ExpressionContext> {
  @Override
  public boolean test(final EK9Parser.ExpressionContext ctx) {

    return assessExpression(ctx);
  }

  private boolean assessExpression(final EK9Parser.ExpressionContext ctx) {

    if (ctx.expression().isEmpty()) {
      //primary can be '(' expression ')'
      if (ctx.primary() != null && ctx.primary().expression() != null) {
        return assessExpression(ctx.primary().expression());
      }
      //Or it is just simple so that's fine.
      return true;
    } else if (ctx.expression().size() == 2 && ctx.AND() != null) {
      //If there are two expressions with an 'and' then assess both sides.
      return assessExpression(ctx.expression(0)) && assessExpression(ctx.expression(1));
    } else {
      //This is just a simple '?' is-set check so that's also fine.
      return ctx.expression().size() == 1 && ctx.QUESTION() != null;
    }

    //Otherwise it's too complex with 'not' / 'or' etc. so do not attempt to process it.

  }
}
