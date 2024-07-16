package org.ek9lang.compiler.phase5;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Checks the while statement via the code analysers.
 */
final class ProcessWhileStatement extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.WhileStatementExpressionContext> {
  ProcessWhileStatement(final SymbolsAndScopes symbolsAndScopes,
                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    final var analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
    final var possibleGuardVariable = getGuardExpressionVariable(ctx.preFlowStatement());

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    checkLoopBodyAndReturn(ctx, possibleGuardVariable.isEmpty());

  }

  private void checkLoopBodyAndReturn(final EK9Parser.WhileStatementExpressionContext ctx,
                                      final boolean noGuardExpression) {

    final var analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
    final var isDoWhile = ctx.DO() != null;
    final var outerScope = symbolsAndScopes.getTopScope();
    final var whileScope = symbolsAndScopes.getRecordedScope(ctx);
    final var loopBodyScope = List.of(symbolsAndScopes.getRecordedScope(ctx.instructionBlock()));

    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, loopBodyScope, whileScope));

    //For a do everything that has been initialised can be pulled up, but not if there is a guard condition
    //this is because it makes the do/while conditional of the result of the guard assignment being set.
    if (isDoWhile && noGuardExpression) {
      analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, loopBodyScope, outerScope));
    }

    checkReturningVariableOrError(ctx.returningParam(), whileScope, noGuardExpression);

  }

}
