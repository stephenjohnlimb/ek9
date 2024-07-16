package org.ek9lang.compiler.phase5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Checks the try/catch/finally structure via the code analysers.
 * This is for each variable at this scope, once we know it is initialised in preflow all good.
 * So this below is a cascade of more checks but once we know it's assigned that's it.
 */
final class ProcessTryStatement extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.TryStatementExpressionContext> {


  ProcessTryStatement(final SymbolsAndScopes symbolsAndScopes,
                      final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.TryStatementExpressionContext ctx) {

    final var analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
    final var possibleGuardVariable = getGuardExpressionVariable(ctx.preFlowStatement());

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    checkTryCatchFinallyAndReturn(ctx, possibleGuardVariable.isEmpty());

  }

  private void checkTryCatchFinallyAndReturn(final EK9Parser.TryStatementExpressionContext ctx,
                                             final boolean noGuardExpression) {

    final var analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
    final var tryCatchBlocks = getTryAndCatchBlocks(ctx);
    final var finallyBlock = getFinallyBlock(ctx);
    final var outerScope = symbolsAndScopes.getTopScope();
    final var tryScope = symbolsAndScopes.getRecordedScope(ctx);

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
          symbolsAndScopes.getRecordedScope(ctx.finallyStatementExpression().block().instructionBlock())
      );
    }

    return List.of();
  }

  private List<IScope> getTryAndCatchBlocks(final EK9Parser.TryStatementExpressionContext ctx) {

    final List<IScope> tryAndCatchBlocks = new ArrayList<>();
    //There is NOT always a try instruction block to process
    var tryInstructionCtx = ctx.instructionBlock();
    if (tryInstructionCtx != null) {
      tryAndCatchBlocks.add(symbolsAndScopes.getRecordedScope(tryInstructionCtx));
      if (ctx.catchStatementExpression() != null) {
        var catchInstructionCtx = ctx.catchStatementExpression().instructionBlock();
        tryAndCatchBlocks.add(symbolsAndScopes.getRecordedScope(catchInstructionCtx));
      }
    }
    return tryAndCatchBlocks;
  }
}
