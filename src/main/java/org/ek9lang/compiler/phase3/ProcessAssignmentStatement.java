package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.RefersToSameSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.compiler.tokenizer.IToken;
import org.ek9lang.core.AssertValue;

/**
 * Used in the full resolution phase to check assignments.
 */
final class ProcessAssignmentStatement extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentStatementContext> {
  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final RefersToSameSymbol refersToSameSymbol = new RefersToSameSymbol();
  private final CheckTypesCompatible checkTypesCompatible;
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final ProcessIdentifierOrError processIdentifierOrError;

  /**
   * Check on validity of assignments.
   */
  ProcessAssignmentStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.checkTypesCompatible = new CheckTypesCompatible(symbolAndScopeManagement, errorListener);
    this.resolveMethodOrError = new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError = new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);

    this.processIdentifierOrError
        = new ProcessIdentifierOrError(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.AssignmentStatementContext ctx) {
    //Note that primaryReference is 'this and 'super'.
    //Has to deal with left hand side: primaryReference | identifier | objectAccessExpression
    //With operators: ASSIGN|ASSIGN2|COLON|ASSIGN_UNSET|ADD_ASSIGN|SUB_ASSIGN|DIV_ASSIGN|MUL_ASSIGN|MERGE|REPLACE|COPY
    //Right hand side is: assignmentExpression

    var expressionSymbol = symbolFromContextOrError.apply(ctx.assignmentExpression());
    if (expressionSymbol == null) {
      //So not resolved an an error will have been emitted.
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
      checkLeftAndRight(primaryReferenceExpressionSymbol, new Ek9Token(ctx.op), expressionSymbol);
    }
  }

  private void checkByIdentifier(final EK9Parser.AssignmentStatementContext ctx, final ISymbol expressionSymbol) {
    var identifier = processIdentifierOrError.apply(ctx.identifier());
    if (identifier != null) {
      if (!identifier.isIncomingParameter()) {
        //If we're in a block and we do some form of assignment, reset referenced because otherwise whats the point.
        //There is no use after the assignment, but in the case of an incoming param we could just be altering the
        //value '+=' for example.
        identifier.setReferenced(false);
      }
      checkIdentifierAssignment(identifier, new Ek9Token(ctx.op), expressionSymbol);
    }
  }

  private void checkByObjectAccessExpression(final EK9Parser.AssignmentStatementContext ctx,
                                             final ISymbol expressionSymbol) {
    var objectAccessExpressionSymbol = symbolFromContextOrError.apply(ctx.objectAccessExpression());
    if (objectAccessExpressionSymbol != null) {
      checkLeftAndRight(objectAccessExpressionSymbol, new Ek9Token(ctx.op), expressionSymbol);
    }
  }

  private void checkIdentifierAssignment(ISymbol leftHandSideSymbol, final IToken op,
                                         final ISymbol assignmentExpression) {
    if (!leftHandSideSymbol.isInitialised()) {
      if (operationIsAssignment.test(op)) {
        leftHandSideSymbol.setInitialisedBy(op);
      } else {
        //It a deep operation, but this variable has not been initialised.
        errorListener.semanticError(op, "'" + leftHandSideSymbol.getFriendlyName() + "'",
            ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED);
      }
    }

    checkLeftAndRight(leftHandSideSymbol, op, assignmentExpression);
  }

  private void checkLeftAndRight(final ISymbol leftHandSideSymbol, final IToken op, final ISymbol rightHandSideSymbol) {
    if (leftHandSideSymbol.getType().isPresent() && rightHandSideSymbol.getType().isPresent()) {
      checkTypesCompatible(leftHandSideSymbol, op, rightHandSideSymbol);
      checkNotSelfAssignment(leftHandSideSymbol, op, rightHandSideSymbol);
    }
  }

  private void checkTypesCompatible(final ISymbol leftHandSideSymbol, final IToken op,
                                    final ISymbol rightHandSideSymbol) {
    if (operationIsAssignment.test(op)) {
      //TODO if the :=? must check that the '?' isSet operator is on the left handside.
      checkTypesCompatible.accept(new TypeCompatibilityData(op, leftHandSideSymbol, rightHandSideSymbol));
    } else {
      leftHandSideSymbol.getType().ifPresent(lhsType -> {
        if (lhsType instanceof IAggregateSymbol aggregate) {
          //Need to resolve the operation.
          var search = new MethodSymbolSearch(op.getText()).addTypeParameter(rightHandSideSymbol.getType());
          MethodSearchInScope searchOnAggregate = new MethodSearchInScope(aggregate, search);
          resolveMethodOrError.apply(op, searchOnAggregate);
        } else {
          AssertValue.fail("Compiler error: expecting an aggregate");
        }
      });
    }
  }

  private void checkNotSelfAssignment(final ISymbol leftHandSideSymbol, final IToken op,
                                      final ISymbol rightHandSideSymbol) {
    if (refersToSameSymbol.test(leftHandSideSymbol, rightHandSideSymbol)) {
      var msg = "'" + leftHandSideSymbol.getFriendlyName()
          + "' and '" + rightHandSideSymbol.getSourceToken().getText() + "' :";
      errorListener.semanticError(op, msg, ErrorListener.SemanticClassification.SELF_ASSIGNMENT);
    }
  }
}
