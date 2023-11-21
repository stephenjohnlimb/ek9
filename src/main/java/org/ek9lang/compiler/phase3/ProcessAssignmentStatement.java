package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;

/**
 * Used in the full resolution phase to check assignments.
 * This is a little more complex than it looks. It is important to
 * check 'pure', incoming and returning arguments and also allowing certain operations when in a constructor
 * on an aggregate.
 */
final class ProcessAssignmentStatement extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentStatementContext> {
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final ProcessIdentifierOrError processIdentifierOrError;

  private final CheckLhsAndRhsAssignment checkLeftAndRight;
  private final ProcessIdentifierAssignment processIdentifierAssignment;

  /**
   * Check on validity of assignments.
   */
  ProcessAssignmentStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError
        = new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
    this.processIdentifierOrError
        = new ProcessIdentifierOrError(symbolAndScopeManagement, errorListener);
    this.checkLeftAndRight
        = new CheckLhsAndRhsAssignment(symbolAndScopeManagement, errorListener);
    this.processIdentifierAssignment
        = new ProcessIdentifierAssignment(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.AssignmentStatementContext ctx) {
    //Note that primaryReference is 'this and 'super'.
    //Has to deal with left hand side: primaryReference | identifier | objectAccessExpression
    //With operators: ASSIGN|ASSIGN2|COLON|ASSIGN_UNSET|ADD_ASSIGN|SUB_ASSIGN|DIV_ASSIGN|MUL_ASSIGN|MERGE|REPLACE|COPY
    //Right hand side is: assignmentExpression

    var expressionSymbol = symbolFromContextOrError.apply(ctx.assignmentExpression());
    if (expressionSymbol == null) {
      //So not resolved and an error will have been emitted.
      return;
    }
    if (ctx.primaryReference() != null) {
      checkByPrimaryReference(ctx, expressionSymbol);
    } else if (ctx.identifier() != null) {
      checkByIdentifier(ctx, expressionSymbol);
    } else if (ctx.objectAccessExpression() != null) {
      checkByObjectAccessExpression(ctx, expressionSymbol);
    } else {
      AssertValue.fail("Expecting finite set of operations on assignment " + ctx.start.getLine());
    }
  }

  private void checkByPrimaryReference(final EK9Parser.AssignmentStatementContext ctx, final ISymbol expressionSymbol) {
    var primaryReferenceExpressionSymbol = symbolFromContextOrError.apply(ctx.primaryReference());
    if (primaryReferenceExpressionSymbol != null) {
      var isAssigningToThis = ctx.primaryReference().THIS() != null;
      var data = new AssignmentData(isAssigningToThis,
          new TypeCompatibilityData(new Ek9Token(ctx.op), primaryReferenceExpressionSymbol, expressionSymbol));

      checkLeftAndRight.accept(data);
    }
  }

  private void checkByIdentifier(final EK9Parser.AssignmentStatementContext ctx, final ISymbol expressionSymbol) {
    var identifier = processIdentifierOrError.apply(ctx.identifier());
    if (identifier != null) {
      var data = new AssignmentData(false,
          new TypeCompatibilityData(new Ek9Token(ctx.op), identifier, expressionSymbol));

      processIdentifierAssignment.accept(data);
    }
  }

  private void checkByObjectAccessExpression(final EK9Parser.AssignmentStatementContext ctx,
                                             final ISymbol expressionSymbol) {
    var objectAccessExpressionSymbol = symbolFromContextOrError.apply(ctx.objectAccessExpression());
    if (objectAccessExpressionSymbol != null) {
      var data =
          new AssignmentData(false,
              new TypeCompatibilityData(new Ek9Token(ctx.op), objectAccessExpressionSymbol, expressionSymbol));
      checkLeftAndRight.accept(data);
    }
  }

}
