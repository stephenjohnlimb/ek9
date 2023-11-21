package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;

/**
 * Deals with the checking and setting initialised of the identifier (as appropriate).
 * Also ensures the the left hand side is initialised.
 */
final class ProcessIdentifierAssignment extends TypedSymbolAccess implements Consumer<AssignmentData> {
  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final CheckLhsAndRhsAssignment checkLeftAndRight;

  ProcessIdentifierAssignment(SymbolAndScopeManagement symbolAndScopeManagement,
                                        ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
    this.checkLeftAndRight
        = new CheckLhsAndRhsAssignment(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(AssignmentData data) {
    if (!data.typeData().lhs().isIncomingParameter() && !data.typeData().lhs().isPropertyField()) {
      //If we're in a block, and we do some form of assignment, reset referenced because otherwise whats the point.
      //There is no use after the assignment, but in the case of an incoming param we could just be altering the
      //value '+=' for example. For properties, we have no idea on ordering of calls.
      data.typeData().lhs().setReferenced(false);
    }
    if (!data.typeData().lhs().isInitialised()) {
      if (operationIsAssignment.test(data.typeData().location())) {
        data.typeData().lhs().setInitialisedBy(data.typeData().location());
      } else {
        //It a deep operation, but this variable has not been initialised.
        errorListener.semanticError(data.typeData().location(), "'" + data.typeData().lhs().getFriendlyName() + "'",
            ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED);
      }
    }
    checkLeftAndRight.accept(data);
  }
}
