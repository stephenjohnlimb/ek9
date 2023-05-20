package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.main.rules.OperationIsAssignment;
import org.ek9lang.compiler.main.rules.RefersToSameSymbol;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.MethodSearchOnAggregate;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;
import org.ek9lang.core.exception.AssertValue;

/**
 * Used in the full resolution phase to check assignments.
 */
public class CheckAssignmentStatement implements Consumer<EK9Parser.AssignmentStatementContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ErrorListener errorListener;

  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();

  private final RefersToSameSymbol refersToSameSymbol = new RefersToSameSymbol();

  private final CheckTypesCompatible checkTypesCompatible;

  private final ResolveMethodOrError resolveMethodOrError;

  /**
   * Check on validity of assignments.
   */
  public CheckAssignmentStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                                  final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;

    this.checkTypesCompatible = new CheckTypesCompatible(errorListener);
    this.resolveMethodOrError = new ResolveMethodOrError(errorListener);
  }

  @Override
  public void accept(EK9Parser.AssignmentStatementContext ctx) {
    //Note that primaryReference is 'this and 'super'.
    //Has to deal with left hand side: primaryReference | identifier | objectAccessExpression
    //With operators: ASSIGN|ASSIGN2|COLON|ASSIGN_UNSET|ADD_ASSIGN|SUB_ASSIGN|DIV_ASSIGN|MUL_ASSIGN|MERGE|REPLACE|COPY
    //Right hand side is: assignmentExpression

    var expressionSymbol = symbolAndScopeManagement.getRecordedSymbol(ctx.assignmentExpression());
    if (ctx.primaryReference() != null) {
      var primaryReferenceExpression = symbolAndScopeManagement.getRecordedSymbol(ctx.primaryReference());
      if (primaryReferenceExpression == null) {
        emitNotResolved(ctx.primaryReference());
      } else {
        checkLeftAndRight(primaryReferenceExpression, ctx.op, expressionSymbol);
      }
    } else if (ctx.identifier() != null) {
      var resolved = symbolAndScopeManagement.getTopScope().resolve(new SymbolSearch(ctx.identifier().getText()));
      resolved.ifPresentOrElse(identifier -> checkIdentifierAssignment(identifier, ctx.op, expressionSymbol),
          () -> emitNotResolved(ctx.identifier()));
    } else if (ctx.objectAccessExpression() != null) {
      var objectAccessExpression = symbolAndScopeManagement.getRecordedSymbol(ctx.objectAccessExpression());
      if (objectAccessExpression == null) {
        emitNotResolved(ctx.objectAccessExpression());
      } else {
        checkLeftAndRight(objectAccessExpression, ctx.op, expressionSymbol);
      }
    }
  }

  private void emitNotResolved(final ParserRuleContext ctx) {
    errorListener.semanticError(ctx.start, "", ErrorListener.SemanticClassification.NOT_RESOLVED);
  }

  private void checkIdentifierAssignment(ISymbol leftHandSideSymbol, final Token op,
                                         final ISymbol assignmentExpression) {
    if (assignmentExpression == null) {
      //Could be null if there is an ek9 code developer error.
      return;
    }

    if (operationIsAssignment.test(op)) {
      if (!leftHandSideSymbol.isInitialised()) {
        leftHandSideSymbol.setInitialisedBy(op);
      }
    } else if (!leftHandSideSymbol.isInitialised()) {
      //It a deep operation, but this variable has not been initialised.
      errorListener.semanticError(leftHandSideSymbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED);
    }

    checkLeftAndRight(leftHandSideSymbol, op, assignmentExpression);
  }

  private void checkLeftAndRight(final ISymbol leftHandSideSymbol, final Token op, final ISymbol rightHandSideSymbol) {
    if (leftHandSideSymbol.getType().isPresent() && rightHandSideSymbol.getType().isPresent()) {
      checkTypesCompatible(leftHandSideSymbol, op, rightHandSideSymbol);
      checkNotSelfAssignment(leftHandSideSymbol, op, rightHandSideSymbol);
    }
  }

  private void checkTypesCompatible(final ISymbol leftHandSideSymbol, final Token op,
                                    final ISymbol rightHandSideSymbol) {
    if (operationIsAssignment.test(op)) {
      checkTypesCompatible.accept(new TypeCompatibilityData(op, leftHandSideSymbol, rightHandSideSymbol));
    } else {
      leftHandSideSymbol.getType().ifPresent(lhsType -> {
        if (lhsType instanceof IAggregateSymbol aggregate) {
          //Need to resolve the operation.
          var search = new MethodSymbolSearch(op.getText()).addTypeParameter(rightHandSideSymbol.getType());
          MethodSearchOnAggregate searchOnAggregate = new MethodSearchOnAggregate(aggregate, search);
          resolveMethodOrError.apply(op, searchOnAggregate);
        } else {
          AssertValue.fail("Compiler error: expecting an aggregate");
        }
      });
    }
  }

  private void checkNotSelfAssignment(final ISymbol leftHandSideSymbol, final Token op,
                                      final ISymbol rightHandSideSymbol) {
    if (refersToSameSymbol.test(leftHandSideSymbol, rightHandSideSymbol)) {
      var msg = "'" + leftHandSideSymbol.getFriendlyName()
          + "' and '" + rightHandSideSymbol.getSourceToken().getText() + "' :";
      errorListener.semanticError(op, msg, ErrorListener.SemanticClassification.SELF_ASSIGNMENT);
    }
  }
}
