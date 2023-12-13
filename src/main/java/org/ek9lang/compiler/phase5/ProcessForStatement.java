package org.ek9lang.compiler.phase5;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;

/**
 * Checks the for structure via the code analysers.
 */
final class ProcessForStatement extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.ForStatementExpressionContext> {
  ProcessForStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.ForStatementExpressionContext ctx) {

    var analyzers = symbolAndScopeManagement.getCodeFlowAnalyzers();
    var possibleGuardVariable = getGuardExpressionVariable(getPreFlowStatement(ctx));

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    checkLoopBodyAndReturn(ctx, possibleGuardVariable.isEmpty());

  }

  private void checkLoopBodyAndReturn(final EK9Parser.ForStatementExpressionContext ctx,
                                      final boolean noGuardExpression) {
    var analyzers = symbolAndScopeManagement.getCodeFlowAnalyzers();

    final var forScope = symbolAndScopeManagement.getRecordedScope(ctx);

    //Note that none of the body alters the outer loop, only the possible guard expression does that.
    var loopBodyScope = List.of(symbolAndScopeManagement.getRecordedScope(ctx.instructionBlock()));
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, loopBodyScope, forScope));

    checkReturningVariableOrError(ctx.returningParam(), forScope, noGuardExpression);

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
