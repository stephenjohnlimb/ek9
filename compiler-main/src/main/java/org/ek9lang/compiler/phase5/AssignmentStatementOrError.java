package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.USED_BEFORE_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.OperationIsAssignment;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.common.UninitialisedVariableToBeChecked;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Check the assignment to a variable can be a single assignment or a deep copy type assignment.
 * The initialised status of the variable may be modified and will be checked as appropriate.
 */
final class AssignmentStatementOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.AssignmentStatementContext> {

  private final OperationIsAssignment operationIsAssignment = new OperationIsAssignment();
  private final UninitialisedVariableToBeChecked uninitialisedVariableToBeChecked =
      new UninitialisedVariableToBeChecked();

  AssignmentStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                             final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  /**
   * Can be other things besides identifier, only really focussed on locals and return variables for this.
   * primaryReference | identifier | objectAccessExpression
   */
  @Override
  public void accept(final EK9Parser.AssignmentStatementContext ctx) {

    if (ctx.identifier() != null) {
      final var symbol = symbolsAndScopes.getRecordedSymbol(ctx.identifier());
      generalAssignmentOrError(ctx, symbol);
    }

  }

  private void generalAssignmentOrError(final EK9Parser.AssignmentStatementContext ctx, final ISymbol symbol) {

    if (uninitialisedVariableToBeChecked.test(symbol)) {
      if (operationIsAssignment.test(new Ek9Token(ctx.op))) {
        simpleAssignment(symbol);
      } else {
        deepAssignmentOrError(ctx, symbol);
      }
    }

  }

  private void simpleAssignment(final ISymbol symbol) {

    symbolsAndScopes.recordSymbolAssignment(symbol);

  }

  /**
   * It's a deep sort of assignment that requires the variable to have been initialised.
   * So if not - we'd issue an error here.
   */
  private void deepAssignmentOrError(final EK9Parser.AssignmentStatementContext ctx, final ISymbol symbol) {

    final var initialised = symbolsAndScopes.isVariableInitialised(symbol);
    if (!initialised) {
      errorListener.semanticError(ctx.start, "'" + symbol.getFriendlyName() + "':", USED_BEFORE_INITIALISED);
    }

  }
}
