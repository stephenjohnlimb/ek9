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
 * This is a little more complex than it looks. It is important to
 * check 'pure', incoming and returning arguments and also allowing certain operations when in a constructor
 * on an aggregate.
 */
final class ProcessAssignmentStatement extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentStatementContext> {
  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final RefersToSameSymbol refersToSameSymbol = new RefersToSameSymbol();
  private final CheckIsPropertyOfAggregate checkIsPropertyOfAggregate = new CheckIsPropertyOfAggregate();
  private final CheckTypesCompatible checkTypesCompatible;
  private final ResolveMethodOrError resolveMethodOrError;
  private final SymbolFromContextOrError symbolFromContextOrError;
  private final ProcessIdentifierOrError processIdentifierOrError;
  private final CheckAssignment checkAssignment;

  /**
   * Check on validity of assignments.
   */
  ProcessAssignmentStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    this.checkTypesCompatible
        = new CheckTypesCompatible(symbolAndScopeManagement, errorListener);
    this.resolveMethodOrError
        = new ResolveMethodOrError(symbolAndScopeManagement, errorListener);
    this.symbolFromContextOrError
        = new SymbolFromContextOrError(symbolAndScopeManagement, errorListener);
    this.processIdentifierOrError
        = new ProcessIdentifierOrError(symbolAndScopeManagement, errorListener);
    this.checkAssignment
        = new CheckAssignment(symbolAndScopeManagement, errorListener);
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
      checkLeftAndRight(isAssigningToThis, primaryReferenceExpressionSymbol, new Ek9Token(ctx.op), expressionSymbol);
    }
  }

  private void checkByIdentifier(final EK9Parser.AssignmentStatementContext ctx, final ISymbol expressionSymbol) {
    var identifier = processIdentifierOrError.apply(ctx.identifier());
    if (identifier != null) {
      if (!identifier.isIncomingParameter() && !identifier.isPropertyField()) {
        //If we're in a block and we do some form of assignment, reset referenced because otherwise whats the point.
        //There is no use after the assignment, but in the case of an incoming param we could just be altering the
        //value '+=' for example. For properties, we have no idea on ordering of calls.
        identifier.setReferenced(false);
      }
      checkIdentifierAssignment(identifier, new Ek9Token(ctx.op), expressionSymbol);
    }
  }

  private void checkByObjectAccessExpression(final EK9Parser.AssignmentStatementContext ctx,
                                             final ISymbol expressionSymbol) {
    var objectAccessExpressionSymbol = symbolFromContextOrError.apply(ctx.objectAccessExpression());
    if (objectAccessExpressionSymbol != null) {
      checkLeftAndRight(false, objectAccessExpressionSymbol, new Ek9Token(ctx.op), expressionSymbol);
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

    checkLeftAndRight(false, leftHandSideSymbol, op, assignmentExpression);
  }

  private void checkLeftAndRight(final boolean isAssigningToThis,
                                 final ISymbol leftHandSideSymbol,
                                 final IToken op,
                                 final ISymbol rightHandSideSymbol) {
    if (leftHandSideSymbol.getType().isPresent() && rightHandSideSymbol.getType().isPresent()) {

      checkTypesCompatible(isAssigningToThis, leftHandSideSymbol, op, rightHandSideSymbol);
      checkNotSelfAssignment(leftHandSideSymbol, op, rightHandSideSymbol);

    }
  }


  private void checkTypesCompatible(final boolean isAssigningToThis,
                                    final ISymbol leftHandSideSymbol,
                                    final IToken op,
                                    final ISymbol rightHandSideSymbol) {
    if (operationIsAssignment.test(op)) {
      checkAssignment.accept(op, leftHandSideSymbol);
      checkTypesCompatible.accept(new TypeCompatibilityData(op, leftHandSideSymbol, rightHandSideSymbol));
    } else {
      leftHandSideSymbol.getType().ifPresent(lhsType -> {
        if (lhsType instanceof IAggregateSymbol aggregate) {
          //Need to resolve the operation.
          var search = new MethodSymbolSearch(op.getText()).addTypeParameter(rightHandSideSymbol.getType());
          MethodSearchInScope searchOnAggregate = new MethodSearchInScope(aggregate, search);
          var resolved = resolveMethodOrError.apply(op, searchOnAggregate);
          if (resolved != null) {
            //Only if the operator is there, do we check.
            checkMutationOperation(isAssigningToThis, leftHandSideSymbol, op);
          }
        } else if (isAssigningToThis) {
          errorListener.semanticError(op, "",
              ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_THIS);

        } else {
          //Now we do allow use of this and super for other types like functions and applications
          //this is so they can reference themselves and pass their type on. So this is not a defect and needs to work.
          AssertValue.fail("Compiler error: expecting an aggregate ["
              + leftHandSideSymbol.getFriendlyName() + "] line no " + op.getLine());
        }
      });
    }
  }

  /**
   * So if there is a mutation operation, then must check if in a pure context.
   * If it is in a pure context, need to take into account whether it is in a constructor.
   */
  private void checkMutationOperation(final boolean isAssigningToThis,
                                      final ISymbol leftHandSideSymbol,
                                      final IToken op) {
    if (isProcessingScopePure()) {
      var maybeMethod = symbolAndScopeManagement.traverseBackUpStackToEnclosingMethod();

      if (maybeMethod.isEmpty()) {
        //So this is not within a method in an aggregate, so no mutation of anything.
        errorListener.semanticError(op, "", ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT);
        return;
      }

      maybeMethod.ifPresent(data -> {
        //Not in constructor - then no mutation in pure
        //If in a constructor then mutation of this or a property on this (mutation)
        if (!maybeMethod.get().methodSymbol().isConstructor()
            || (!isAssigningToThis && !checkIsPropertyOfAggregate.test(maybeMethod.get().aggregateSymbol(),
            leftHandSideSymbol))) {
          errorListener.semanticError(op, "", ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT);
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
