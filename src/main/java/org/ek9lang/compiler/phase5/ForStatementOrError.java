package org.ek9lang.compiler.phase5;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.CodeFlowAnalyzer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;

/**
 * Checks the for structure via the code analysers.
 */
final class ForStatementOrError extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.ForStatementExpressionContext> {
  private final List<CodeFlowAnalyzer> analyzers;

  ForStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                      final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
    this.analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
  }

  @Override
  public void accept(final EK9Parser.ForStatementExpressionContext ctx) {

    final var possibleGuardVariable = getGuardExpressionVariable(getPreFlowStatement(ctx));

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    loopBodyAndReturnValidOrError(ctx, possibleGuardVariable.isEmpty());

  }

  private void loopBodyAndReturnValidOrError(final EK9Parser.ForStatementExpressionContext ctx,
                                             final boolean noGuardExpression) {
    
    final var forScope = symbolsAndScopes.getRecordedScope(ctx);
    //Note that none of the body alters the outer loop, only the possible guard expression does that.
    final var loopBodyScope = List.of(symbolsAndScopes.getRecordedScope(ctx.instructionBlock()));

    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, loopBodyScope, forScope));

    returningVariableValidOrError(ctx.returningParam(), forScope, noGuardExpression);

  }

  private EK9Parser.PreFlowStatementContext getPreFlowStatement(final EK9Parser.ForStatementExpressionContext ctx) {

    if (ctx.forLoop() != null
        && ctx.forLoop().preFlowStatement() != null) {
      return ctx.forLoop().preFlowStatement();
    }

    if (ctx.forRange() != null
        && ctx.forRange().preFlowStatement() != null) {
      return ctx.forRange().preFlowStatement();
    }

    return null;
  }
}
