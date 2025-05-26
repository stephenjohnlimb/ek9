package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.CheckReturns;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Ensures that a for loop/for range statement/expression is used correctly in or out of an expression.
 */
final class ForReturnOrError extends CheckReturns implements Consumer<EK9Parser.ForStatementExpressionContext> {

  ForReturnOrError(final ErrorListener errorListener) {

    super(errorListener);

  }

  @Override
  public void accept(final EK9Parser.ForStatementExpressionContext ctx) {

    returningParamOrError(ctx.parent instanceof EK9Parser.StatementContext, new Ek9Token(ctx.start),
        ctx.returningParam());

  }
}
