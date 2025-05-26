package org.ek9lang.compiler.phase5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.CodeFlowAnalyzer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IScope;

/**
 * Deals with checking if all paths through switch/case/default (return) result in variables meeting criteria.
 * If they do then the outer variable meta-data can also be marked as also meeting the criteria.
 * Initially, this was just were variables initialised. But now the introduction of CodeFlowAnalyzers
 * means that several forms of symbol analysis can take place.
 */
final class SwitchStatementOrError extends PossibleExpressionConstruct
    implements Consumer<EK9Parser.SwitchStatementExpressionContext> {

  private final List<CodeFlowAnalyzer> analyzers;

  SwitchStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                         final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.analyzers = symbolsAndScopes.getCodeFlowAnalyzers();

  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {

    final var possibleGuardVariable = getGuardExpressionVariable(ctx.preFlowAndControl());

    possibleGuardVariable.ifPresent(guardVariable ->
        analyzers.forEach(analyzer -> processPossibleGuardInitialisation(analyzer, guardVariable, ctx)));

    casesDefaultAndReturnValidOrError(ctx, possibleGuardVariable.isEmpty());

  }

  private void casesDefaultAndReturnValidOrError(final EK9Parser.SwitchStatementExpressionContext ctx,
                                                 final boolean noGuardExpression) {

    final var allBlocks = getAllBlocks(ctx);
    final var outerScope = symbolsAndScopes.getTopScope();
    final var switchScope = symbolsAndScopes.getRecordedScope(ctx);

    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, allBlocks, switchScope));

    if (noGuardExpression) {
      analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, allBlocks, outerScope));
    }

    returningVariableValidOrError(ctx.returningParam(), switchScope, noGuardExpression);

  }

  /**
   * Get instruction blocks of the cases and the default.
   */
  private List<IScope> getAllBlocks(final EK9Parser.SwitchStatementExpressionContext ctx) {

    final List<IScope> allBlocks = new ArrayList<>();

    //Which it can be if there is a returning variable as part of the switch. This is the optional 'default'.
    if (ctx.block() != null) {
      allBlocks.add(symbolsAndScopes.getRecordedScope(ctx.block().instructionBlock()));
    }

    final var allCaseInstructionBlocks =
        ctx.caseStatement().stream()
            .map(ifControl -> ifControl.block().instructionBlock())
            .map(symbolsAndScopes::getRecordedScope)
            .toList();

    allBlocks.addAll(allCaseInstructionBlocks);

    return allBlocks;
  }

}
