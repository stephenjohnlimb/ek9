package org.ek9lang.compiler.support;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Ensures that a try statement/expression is used correctly in or out of an expression.
 */
public class CheckTryReturns extends CheckReturns implements Consumer<EK9Parser.TryStatementExpressionContext> {

  CheckTryReturns(final ErrorListener errorListener) {

    super(errorListener);

  }

  @Override
  public void accept(final EK9Parser.TryStatementExpressionContext ctx) {

    returningParamOrError(ctx.parent instanceof EK9Parser.StatementContext, new Ek9Token(ctx.start),
        ctx.returningParam());

  }
}
