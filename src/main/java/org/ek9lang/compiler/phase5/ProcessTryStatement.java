package org.ek9lang.compiler.phase5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Checks the try/catch/finally structure via the code analysers.
 * This is for each variable at this scope, once we know it is initialised in preflow all good.
 * So this below is a cascade of more checks but once we know it's assigned that's it.
 */
final class ProcessTryStatement extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.TryStatementExpressionContext> {


  ProcessTryStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.TryStatementExpressionContext ctx) {
    var analyzers = symbolAndScopeManagement.getCodeFlowAnalyzers();
    var possibleGuardVariable = getGuardExpressionVariable(ctx.preFlowStatement());

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    checkTryCatchFinallyAndReturn(ctx, possibleGuardVariable.isEmpty());
  }

  private void checkTryCatchFinallyAndReturn(final EK9Parser.TryStatementExpressionContext ctx,
                                             final boolean noGuardExpression) {
    var analyzers = symbolAndScopeManagement.getCodeFlowAnalyzers();

    List<IScope> tryCatchBlocks = getTryAndCatchBlocks(ctx);
    List<IScope> finallyBlock = getFinallyBlock(ctx);

    final var outerScope = symbolAndScopeManagement.getTopScope();
    final var tryScope = symbolAndScopeManagement.getRecordedScope(ctx);

    //This is the outer scope where it may be possible to mark a variable as meeting criteria
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, tryCatchBlocks, tryScope));
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, finallyBlock, tryScope));

    if (noGuardExpression) {
      analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, tryCatchBlocks, outerScope));
      analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, finallyBlock, outerScope));
    }

    checkReturningVariableOrError(ctx.returningParam(), tryScope, noGuardExpression);

  }

  private List<IScope> getFinallyBlock(EK9Parser.TryStatementExpressionContext ctx) {

    if (ctx.finallyStatementExpression() != null) {
      return List.of(
          symbolAndScopeManagement.getRecordedScope(ctx.finallyStatementExpression().block().instructionBlock())
      );
    }
    return List.of();

  }

  private List<IScope> getTryAndCatchBlocks(final EK9Parser.TryStatementExpressionContext ctx) {

    List<IScope> tryAndCatchBlocks = new ArrayList<>();
    //There is always a try instruction block to process
    tryAndCatchBlocks.add(symbolAndScopeManagement.getRecordedScope(ctx.instructionBlock()));
    if (ctx.catchStatementExpression() != null) {
      tryAndCatchBlocks.add(
          symbolAndScopeManagement.getRecordedScope(ctx.catchStatementExpression().instructionBlock())
      );
    }
    return tryAndCatchBlocks;

  }
}
