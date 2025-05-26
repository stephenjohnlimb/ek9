package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.CheckReturns;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Ensures that a while do/while statement/expression is used correctly in or out of an expression.
 */
final class WhileReturnOrError extends CheckReturns implements Consumer<EK9Parser.WhileStatementExpressionContext> {

  WhileReturnOrError(final ErrorListener errorListener) {

    super(errorListener);

  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    returningParamOrError(ctx.parent instanceof EK9Parser.StatementContext, new Ek9Token(ctx.start),
        ctx.returningParam());

  }
}
