package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Check an assignment again 'super' use and some operators against 'this' use.
 */
final class CheckThisAndSuperAssignmentStatement implements Consumer<EK9Parser.AssignmentStatementContext> {

  private final ErrorListener errorListener;

  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();

  CheckThisAndSuperAssignmentStatement(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(final EK9Parser.AssignmentStatementContext ctx) {

    if (ctx.primaryReference() != null) {
      //no assignment allowed and 'super' use is not appropriate only 'this'
      final var accessType = ctx.primaryReference().getText();
      if (operationIsAssignment.test(new Ek9Token(ctx.op))) {
        errorListener.semanticError(ctx.primaryReference().start, "'" + accessType + "'",
            ErrorListener.SemanticClassification.USE_OF_THIS_OR_SUPER_INAPPROPRIATE);
      }
    }

  }
}
