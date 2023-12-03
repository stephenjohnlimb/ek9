package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.UninitialisedVariableToBeChecked;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Check the assignment to a variable can be a single assignment or a deep copy type assignment.
 * The initialised status of the variable may be modified and will be checked as appropriate.
 */
final class ProcessAssignmentStatement extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentStatementContext> {

  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  ProcessAssignmentStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                             final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  /**
   * Can be other things besides identifier, only really focussed on locals and return variables for this.
   * primaryReference | identifier | objectAccessExpression
   */
  @Override
  public void accept(final EK9Parser.AssignmentStatementContext ctx) {

    if (ctx.identifier() != null) {
      var symbol = symbolAndScopeManagement.getRecordedSymbol(ctx.identifier());
      checkGeneralAssignment(ctx, symbol);
    }

  }

  private void checkGeneralAssignment(final EK9Parser.AssignmentStatementContext ctx, final ISymbol symbol) {

    if (uninitialisedVariableToBeChecked.test(symbol)) {
      if (operationIsAssignment.test(new Ek9Token(ctx.op))) {
        simpleAssignment(symbol);
      } else {
        checkDeepAssignment(ctx, symbol);
      }
    }

  }

  private void simpleAssignment(final ISymbol symbol) {

    symbolAndScopeManagement.recordSymbolAssignment(symbol);

  }

  /**
   * It's a deep sort of assignment that requires the variable to have been initialised.
   * So if not - we'd issue an error here.
   */
  private void checkDeepAssignment(final EK9Parser.AssignmentStatementContext ctx, final ISymbol symbol) {

    var initialised = symbolAndScopeManagement.isVariableInitialised(symbol);
    if (!initialised) {
      errorListener.semanticError(ctx.start, "'" + symbol.getFriendlyName() + "':", USED_BEFORE_INITIALISED);
    }

  }
}
