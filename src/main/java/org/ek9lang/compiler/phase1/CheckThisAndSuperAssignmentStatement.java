package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;

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
  public void accept(EK9Parser.AssignmentStatementContext ctx) {
    if (ctx.primaryReference() != null) {
      //no assignment allowed and 'super' use is not appropriate only 'this'
      if (ctx.primaryReference().SUPER() != null) {
        errorListener.semanticError(ctx.primaryReference().start, "'super'",
            ErrorListener.SemanticClassification.USE_OF_SUPER_INAPPROPRIATE);
      } else {
        //Basically we allow a sort of assignment that accepts this 'this' has a value but can be mutated.
        //For example the merge operator or, copy (and others) but NOT these below.
        if (operationIsAssignment.test(ctx.op)) {
          errorListener.semanticError(ctx.primaryReference().start, "'this'",
              ErrorListener.SemanticClassification.USE_OF_THIS_INAPPROPRIATE);
        }
      }
    }
  }
}
