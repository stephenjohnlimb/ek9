package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.RETURN_NOT_ALWAYS_INITIALISED;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.CodeFlowAnalyzer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Deals with checking if all paths through switch/case/default (return) result in variables meeting criteria.
 * If they do then the outer variable meta-data can also be marked as also meeting the criteria.
 * Initially, this was just were variables initialised. But now the introduction of CodeFlowAnalyzers
 * means that several forms of symbol analysis can take place.
 */
final class ProcessSwitchStatement extends TypedSymbolAccess
    implements Consumer<EK9Parser.SwitchStatementExpressionContext> {
  ProcessSwitchStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                         final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.SwitchStatementExpressionContext ctx) {
    var analyzers = symbolAndScopeManagement.getCodeFlowAnalyzers();

    //So lets check to see if a variable was initialised in the first guard (only the first guard though).
    analyzers.forEach(analyzer -> processPossibleFirstIfGuardInitialisation(analyzer, ctx));

    //And these are all the if else-if else blocks that have to be checked.
    List<IScope> allBlocks = getAllBlocks(ctx);
    //This is the outer scope where it may be possible to mark a variable as meeting criteria
    final var outerScope = symbolAndScopeManagement.getTopScope();
    final var switchScope = symbolAndScopeManagement.getRecordedScope(ctx);

    //So these are the variables we need to check to see if we can mark them as meeting the criteria.
    //Note need to do both because rtn is in the switch, but other variables maybe in the outer scope.
    //This is different to the 'if/else' as that does not have any returns.
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, allBlocks, switchScope));
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, allBlocks, outerScope));

    if (ctx.returningParam() != null) {
      //Note that the returning variable is registered against the switch scope.
      var returningVariable = symbolAndScopeManagement.getRecordedSymbol(ctx.returningParam());
      if (!symbolAndScopeManagement.isVariableInitialised(returningVariable, switchScope)) {
        errorListener.semanticError(ctx.returningParam().LEFT_ARROW().getSymbol(),
            "'" + returningVariable.getName() + "':", RETURN_NOT_ALWAYS_INITIALISED);
      }
    }
  }

  private void pullUpAcceptableCriteriaToHigherScope(final CodeFlowAnalyzer analyzer,
                                                     List<IScope> allIfElseBlocks,
                                                     final IScope outerScope) {
    var unInitialisedVariables = analyzer.getSymbolsNotMeetingAcceptableCriteria(outerScope);
    for (var variable : unInitialisedVariables) {
      if (isVariableInitialisedInEveryScope(analyzer, variable, allIfElseBlocks)) {
        analyzer.markSymbolAsMeetingAcceptableCriteria(variable, outerScope);
      }
    }
  }

  /**
   * Now when there is a preflow control, it may mean that a variable is initialised.
   */
  private void processPossibleFirstIfGuardInitialisation(final CodeFlowAnalyzer analyzer,
                                                         final EK9Parser.SwitchStatementExpressionContext ctx) {

    if (ctx.preFlowAndControl() != null
        && ctx.preFlowAndControl().preFlowStatement() != null
        && ctx.preFlowAndControl().preFlowStatement().guardExpression() != null) {

      var variable = symbolAndScopeManagement.getRecordedSymbol(
          ctx.preFlowAndControl().preFlowStatement().guardExpression().identifier());
      var scope = symbolAndScopeManagement.getRecordedScope(ctx);

      boolean initialised = analyzer.doesSymbolMeetAcceptableCriteria(variable, scope);
      if (initialised) {
        final var outerScope = symbolAndScopeManagement.getTopScope();
        analyzer.markSymbolAsMeetingAcceptableCriteria(variable, outerScope);
      }
    }

  }

  private boolean isVariableInitialisedInEveryScope(final CodeFlowAnalyzer analyzer,
                                                    final ISymbol variable,
                                                    final List<IScope> scopes) {

    return scopes.stream()
        .allMatch(scope -> analyzer.doesSymbolMeetAcceptableCriteria(variable, scope));

  }

  /**
   * Get instruction blocks of the ifs and also the else.
   */
  private List<IScope> getAllBlocks(final EK9Parser.SwitchStatementExpressionContext ctx) {

    List<IScope> allBlocks = new ArrayList<>();
    //Which it can be if there is a returning variable as part of the switch.
    if (ctx.block() != null) {
      allBlocks.add(symbolAndScopeManagement.getRecordedScope(ctx.block().instructionBlock()));
    }

    var allCaseInstructionBlocks =
        ctx.caseStatement().stream()
            .map(ifControl -> ifControl.block().instructionBlock())
            .map(symbolAndScopeManagement::getRecordedScope)
            .toList();
    allBlocks.addAll(allCaseInstructionBlocks);
    return allBlocks;
  }

}
