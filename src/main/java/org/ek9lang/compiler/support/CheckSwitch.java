package org.ek9lang.compiler.support;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Ensures that a switch is used correctly in or out of an expression.
 * This now includes checking for 'default' and it's correct use with statement and expression switches.
 */
public class CheckSwitch extends CheckReturns implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  CheckSwitch(final ErrorListener errorListener) {
    super(errorListener);
  }

  @Override
  public void accept(EK9Parser.SwitchStatementExpressionContext ctx) {
    boolean isStatement = ctx.parent instanceof EK9Parser.StatementContext;
    check(isStatement, new Ek9Token(ctx.start), ctx.returningParam());
    checkDefault(isStatement, ctx);
  }

  private void checkDefault(final boolean isStatement, EK9Parser.SwitchStatementExpressionContext ctx) {

    if (ctx.DEFAULT() == null) {
      //So no default and for a statement this is an error
      if (isStatement) {
        errorListener.semanticError(ctx.start, "",
            ErrorListener.SemanticClassification.DEFAULT_REQUIRED_IN_SWITCH_STATEMENT);

      } else if (ctx.returningParam() != null
          && ctx.returningParam().variableOnlyDeclaration() != null
          && ctx.returningParam().variableOnlyDeclaration().QUESTION() != null) {
        //This too is an error, because the return has not been initialised a 'default is now required.
        errorListener.semanticError(ctx.start, "",
            ErrorListener.SemanticClassification.DEFAULT_REQUIRED_IN_SWITCH_EXPRESSION);

      }
    }
  }
}
