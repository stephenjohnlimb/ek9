package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.support.CheckReturns;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Ensures that a switch is used correctly in or out of an expression.
 * This now includes checking for 'default' and it's correct use with statement and expression switches.
 */
final class SwitchOrError extends CheckReturns implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  SwitchOrError(final ErrorListener errorListener) {
    super(errorListener);
  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {

    boolean isStatement = ctx.parent instanceof EK9Parser.StatementContext;
    returningParamOrError(isStatement, new Ek9Token(ctx.start), ctx.returningParam());

  }
}
