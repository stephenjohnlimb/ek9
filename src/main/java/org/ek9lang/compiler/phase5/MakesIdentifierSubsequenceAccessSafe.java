package org.ek9lang.compiler.phase5;

import java.util.function.Predicate;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * A bit of a catch 22, need to issue errors and the like when traversing an identifier in
 * some circumstances. But not if the identifierReference is actually being used in a 'is-set' test.
 * But it maybe that this is an assignment which then makes subsequence access SAFE.
 * So I don't like doing this sort of thing because it makes the grammar fragile. But here I need to traverse back up
 * the grammar structure to see if this identifier is being used as part of a '?' expression or a safe assignment.
 */
final class MakesIdentifierSubsequenceAccessSafe implements Predicate<EK9Parser.IdentifierContext> {

  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();

  @Override
  public boolean test(final EK9Parser.IdentifierContext ctx) {

    if (ctx.getParent() instanceof EK9Parser.IdentifierReferenceContext
        && ctx.getParent().getParent() instanceof EK9Parser.PrimaryContext
        && ctx.getParent().getParent().getParent() instanceof EK9Parser.ExpressionContext
        && ctx.getParent().getParent().getParent().getParent()
        instanceof EK9Parser.ExpressionContext possibleIsSetExpression) {

      return possibleIsSetExpression.QUESTION() != null;
    } else if (ctx.getParent() instanceof EK9Parser.AssignmentStatementContext assignmentStatement) {

      return operationIsAssignment.test(new Ek9Token(assignmentStatement.op));
    }

    return (ctx.getParent() instanceof EK9Parser.GuardExpressionContext);
  }

}
