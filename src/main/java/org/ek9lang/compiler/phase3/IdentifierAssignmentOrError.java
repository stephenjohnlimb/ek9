package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.support.CommonValues.NO_REFERENCED_RESET;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;

/**
 * Deals with the checking and setting initialised of the identifier.
 */
final class IdentifierAssignmentOrError extends TypedSymbolAccess implements Consumer<AssignmentData> {
  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final LhsAndRhsAssignmentOrError validLeftAndRightOrError;

  IdentifierAssignmentOrError(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.validLeftAndRightOrError = new LhsAndRhsAssignmentOrError(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final AssignmentData data) {

    if (!data.typeData().lhs().isIncomingParameter() && !data.typeData().lhs().isPropertyField()) {
      //If we're in a block, and we do some form of assignment, reset referenced because otherwise what's the point.
      //There is no use after the assignment, but in the case of an incoming param we could just be altering the
      //value '+=' for example. For properties, we have no idea on ordering of calls.
      //But also need to consider a 'while' loop and the assignment happens in the block. This is prevented by
      //NO_REFERENCED_RESET being applied.
      final var lhsSymbol = data.typeData().lhs();
      if (!"TRUE".equals(lhsSymbol.getSquirrelledData(NO_REFERENCED_RESET))) {
        data.typeData().lhs().setReferenced(false);
      }

    }

    if (!data.typeData().lhs().isInitialised() && operationIsAssignment.test(data.typeData().location())) {
      data.typeData().lhs().setInitialisedBy(data.typeData().location());
    }

    validLeftAndRightOrError.accept(data);

  }
}
