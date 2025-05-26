package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.CodeFlowAnalyzer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Checks the while statement via the code analysers.
 */
final class WhileStatementOrError extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.WhileStatementExpressionContext> {

  private final List<CodeFlowAnalyzer> analyzers;

  WhileStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                        final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
  }

  @Override
  public void accept(final EK9Parser.WhileStatementExpressionContext ctx) {

    final var possibleGuardVariable = getGuardExpressionVariable(ctx.preFlowStatement());

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    loopBodyAndReturnValidOrError(ctx, possibleGuardVariable.isEmpty());

  }

  private void loopBodyAndReturnValidOrError(final EK9Parser.WhileStatementExpressionContext ctx,
                                             final boolean noGuardExpression) {

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

    returningVariableValidOrError(ctx.returningParam(), whileScope, noGuardExpression);

    //Because of the conditional the returning value 'if defined as uninitialised' - may never actually be given a
    //value, this is because at runtime the condition may be false.
    if (ctx.returningParam() != null
        && ctx.returningParam().variableOnlyDeclaration() != null
        && ctx.returningParam().variableOnlyDeclaration().QUESTION() != null) {
      errorListener.semanticError(ctx.returningParam().LEFT_ARROW().getSymbol(),
          "'" + ctx.returningParam().variableOnlyDeclaration().identifier().getText() + "':",
          RETURN_NOT_ALWAYS_INITIALISED);
    }
  }

}
