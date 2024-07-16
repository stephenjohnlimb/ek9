package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSearchInScope;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.support.RefersToSameSymbol;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Checks the left hand side and the right hand side for assignment compatibility.
 */
final class CheckLhsAndRhsAssignment extends TypedSymbolAccess implements Consumer<AssignmentData> {
  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final RefersToSameSymbol refersToSameSymbol = new RefersToSameSymbol();
  private final CheckIsPropertyOfAggregate checkIsPropertyOfAggregate = new CheckIsPropertyOfAggregate();
  private final CheckTypesCompatible checkTypesCompatible;
  private final ResolveMethodOrError resolveMethodOrError;
  private final CheckAssignment checkAssignment;
  private final CheckMutableOrError checkMutableOrError;

  CheckLhsAndRhsAssignment(final SymbolsAndScopes symbolsAndScopes,
                           final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.checkTypesCompatible
        = new CheckTypesCompatible(symbolsAndScopes, errorListener);
    this.resolveMethodOrError
        = new ResolveMethodOrError(symbolsAndScopes, errorListener);
    this.checkAssignment
        = new CheckAssignment(symbolsAndScopes, errorListener, false);
    this.checkMutableOrError
        = new CheckMutableOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final AssignmentData data) {

    if (data.typeData().lhs().getType().isPresent() && data.typeData().rhs().getType().isPresent()) {
      checkMutableOrError.accept(data.typeData().location(), data.typeData().lhs());
      checkTypesCompatible(data);
      checkNotSelfAssignment(data);
    }

  }

  private void checkTypesCompatible(final AssignmentData data) {

    if (operationIsAssignment.test(data.typeData().location())) {
      checkSimpleAssignment(data);
    } else {
      checkOtherMutations(data);
    }
  }

  private void checkSimpleAssignment(final AssignmentData data) {

    checkAssignment.accept(data.typeData().location(), data.typeData().lhs());
    checkTypesCompatible.accept(
        new TypeCompatibilityData(data.typeData().location(), data.typeData().lhs(), data.typeData().rhs()));

  }

  private void checkOtherMutations(final AssignmentData data) {

    data.typeData().lhs().getType().ifPresent(lhsType -> {

      if (lhsType instanceof IAggregateSymbol aggregate) {
        //Need to resolve the operation.
        final var search =
            new MethodSymbolSearch(data.typeData().location().getText()).addTypeParameter(
                data.typeData().rhs().getType());
        final var searchOnAggregate = new MethodSearchInScope(aggregate, search);
        final var resolved = resolveMethodOrError.apply(data.typeData().location(), searchOnAggregate);

        if (resolved != null) {
          //Only if the operator is there, do we check.
          checkMutationOperation(data);
        }

      } else if (data.isAssigningToThis()) {
        errorListener.semanticError(data.typeData().location(), "",
            ErrorListener.SemanticClassification.INAPPROPRIATE_USE_OF_THIS);

      } else {
        //Now we do allow use of this and super for other types like functions and applications
        //this is so they can reference themselves and pass their type on. So this is not a defect and needs to work.
        AssertValue.fail("Compiler error: expecting an aggregate ["
            + data.typeData().lhs().getFriendlyName() + "] line no " + data.typeData().location().getLine());
      }
    });
  }

  /**
   * So if there is a mutation operation, then must check if in a pure context.
   * If it is in a pure context, need to take into account whether it is in a constructor.
   */
  private void checkMutationOperation(final AssignmentData data) {

    if (isProcessingScopePure()) {
      final var maybeMethod = symbolsAndScopes.traverseBackUpStackToEnclosingMethod();

      if (maybeMethod.isEmpty()) {
        //So this is not within a method in an aggregate, so no mutation of anything.
        errorListener.semanticError(data.typeData().location(), "",
            ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT);
        return;
      }

      maybeMethod.ifPresent(method -> {
        //Not in constructor - then no mutation in pure
        //If in a constructor then mutation of this or a property on this (mutation)
        if (!maybeMethod.get().methodSymbol().isConstructor()
            || (!data.isAssigningToThis() && !checkIsPropertyOfAggregate.test(maybeMethod.get().aggregateSymbol(),
            data.typeData().lhs()))) {
          errorListener.semanticError(data.typeData().location(), "",
              ErrorListener.SemanticClassification.NO_MUTATION_IN_PURE_CONTEXT);
        }
      });

    }
  }

  private void checkNotSelfAssignment(final AssignmentData data) {

    if (refersToSameSymbol.test(data.typeData().lhs(), data.typeData().rhs())) {
      final var msg = "'" + data.typeData().lhs().getFriendlyName()
          + "' and '" + data.typeData().rhs().getSourceToken().getText() + "' :";
      errorListener.semanticError(data.typeData().location(), msg,
          ErrorListener.SemanticClassification.SELF_ASSIGNMENT);
    }

  }
}
