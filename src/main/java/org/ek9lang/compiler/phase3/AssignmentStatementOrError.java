package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Used in the full resolution phase to check assignments.
 * This is a little more complex than it looks. It is important to
 * check 'pure', incoming and returning arguments and also allowing certain operations when in a constructor
 * on an aggregate.
 */
final class AssignmentStatementOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentStatementContext> {
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final IdentifierOrError identifierOrError;
  private final LhsAndRhsAssignmentOrError checkLeftAndRight;
  private final IdentifierAssignmentOrError identifierAssignmentOrError;

  /**
   * Check on validity of assignments.
   */
  AssignmentStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.symbolFromContextOrError
        = new SymbolFromContextOrError(symbolsAndScopes, errorListener);
    this.identifierOrError
        = new IdentifierOrError(symbolsAndScopes, errorListener);
    this.checkLeftAndRight
        = new LhsAndRhsAssignmentOrError(symbolsAndScopes, errorListener);
    this.identifierAssignmentOrError
        = new IdentifierAssignmentOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.AssignmentStatementContext ctx) {
    //Note that primaryReference is 'this and 'super'.
    //Has to deal with left hand side: primaryReference | identifier | objectAccessExpression
    //With operators: ASSIGN|ASSIGN2|COLON|ASSIGN_UNSET|ADD_ASSIGN|SUB_ASSIGN|DIV_ASSIGN|MUL_ASSIGN|MERGE|REPLACE|COPY
    //Right hand side is: assignmentExpression

    final var expressionSymbol = symbolFromContextOrError.apply(ctx.assignmentExpression());
    if (expressionSymbol == null) {
      //So not resolved and an error will have been emitted.
      return;
    }

    if (ctx.primaryReference() != null) {
      processByPrimaryReference(ctx, expressionSymbol);
    } else if (ctx.identifier() != null) {
      processByIdentifier(ctx, expressionSymbol);
    } else if (ctx.objectAccessExpression() != null) {
      processByObjectAccessExpression(ctx, expressionSymbol);
    } else {
      AssertValue.fail("Expecting finite set of operations on assignment " + ctx.start.getLine());
    }

  }

  private void processByPrimaryReference(final EK9Parser.AssignmentStatementContext ctx,
                                         final ISymbol expressionSymbol) {

    final var primaryReferenceExpressionSymbol = symbolFromContextOrError.apply(ctx.primaryReference());
    if (primaryReferenceExpressionSymbol != null) {

      final var isAssigningToThis = ctx.primaryReference().THIS() != null;
      final var data = new AssignmentData(isAssigningToThis,
          new TypeCompatibilityData(new Ek9Token(ctx.op), primaryReferenceExpressionSymbol, expressionSymbol));

      checkLeftAndRight.accept(data);
    }
  }

  private void processByIdentifier(final EK9Parser.AssignmentStatementContext ctx, final ISymbol expressionSymbol) {

    final var identifier = identifierOrError.apply(ctx.identifier());
    if (identifier != null) {
      final var op = new Ek9Token(ctx.op);
      final var typeData = new TypeCompatibilityData(op, identifier, expressionSymbol);
      final var data = new AssignmentData(false, typeData);
      identifierAssignmentOrError.accept(data);
    }
  }

  private void processByObjectAccessExpression(final EK9Parser.AssignmentStatementContext ctx,
                                               final ISymbol expressionSymbol) {

    final var objectAccessExpressionSymbol = symbolFromContextOrError.apply(ctx.objectAccessExpression());
    if (objectAccessExpressionSymbol != null) {
      final var data = new AssignmentData(false,
          new TypeCompatibilityData(new Ek9Token(ctx.op), objectAccessExpressionSymbol, expressionSymbol));

      checkLeftAndRight.accept(data);
    }

  }

}
