package org.ek9lang.compiler.main.rules;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;

/**
 * Ensures that a switch is used correctly in or out of an expression.
 */
public class CheckSwitchReturns implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  private final ErrorListener errorListener;

  public CheckSwitchReturns(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.SwitchStatementExpressionContext ctx) {

    if (ctx.parent instanceof EK9Parser.StatementContext && ctx.returningParam() != null) {
      //We do not allow a return section. As it makes no sense
      errorListener.semanticError(ctx.returningParam().start, "",
          ErrorListener.SemanticClassification.RETURNING_REDUNDANT);
    } else if (ctx.parent instanceof EK9Parser.AssignmentExpressionContext && ctx.returningParam() == null) {
      //In this case it only makes sense if a return section is included.
      errorListener.semanticError(ctx.start, "",
          ErrorListener.SemanticClassification.RETURNING_REQUIRED);
    }
  }
}
