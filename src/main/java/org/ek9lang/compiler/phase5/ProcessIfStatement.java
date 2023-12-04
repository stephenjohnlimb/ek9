package org.ek9lang.compiler.phase5;

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
 * Deals with checking if all paths through if/else/else-if/else result in variables meeting criteria.
 * If they do then the outer variable meta-data can also be marked as also meeting the criteria.
 * Initially, this was just were variables initialised. But now the introduction of CodeFlowAnalyzers
 * means that several forms of symbol analysis can take place.
 */
final class ProcessIfStatement extends TypedSymbolAccess implements Consumer<EK9Parser.IfStatementContext> {
  ProcessIfStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                     final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.IfStatementContext ctx) {
    var analyzers = symbolAndScopeManagement.getCodeFlowAnalyzers();

    //So lets check to see if a variable was initialised in the first guard (only the first guard though).
    analyzers.forEach(analyzer -> processPossibleFirstIfGuardInitialisation(analyzer, ctx));

    if (ctx.elseOnlyBlock() == null) {
      //Then only an 'if' or a set of 'ifs' may/may not have set any of the variable criteria.
      //So we cannot modify the outer scope variables, so we're done
      return;
    }

    //And these are all the if else-if else blocks that have to be checked.
    List<IScope> allIfElseBlocks = getAllBlocks(ctx);
    //This is the outer scope where it may be possible to mark a variable as meeting criteria
    final var outerScope = symbolAndScopeManagement.getTopScope();

    //So these are the variables we need to check to see if we can mark them as meeting the criteria.
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, allIfElseBlocks, outerScope));

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
   * Now when the first 'if' has a guard it is 'always' executed and so may initialise a variable.
   */
  private void processPossibleFirstIfGuardInitialisation(final CodeFlowAnalyzer analyzer,
                                                         final EK9Parser.IfStatementContext ctx) {

    if (ctx.ifControlBlock().get(0).preFlowAndControl() != null
        && ctx.ifControlBlock().get(0).preFlowAndControl().preFlowStatement() != null
        && ctx.ifControlBlock().get(0).preFlowAndControl().preFlowStatement().guardExpression() != null) {

      var variable = symbolAndScopeManagement.getRecordedSymbol(
          ctx.ifControlBlock().get(0).preFlowAndControl().preFlowStatement().guardExpression().identifier());
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
  private List<IScope> getAllBlocks(final EK9Parser.IfStatementContext ctx) {

    List<IScope> allBlocks = new ArrayList<>();
    allBlocks.add(symbolAndScopeManagement.getRecordedScope(ctx.elseOnlyBlock().block().instructionBlock()));

    var allIfInstructionBlocks =
        ctx.ifControlBlock().stream()
            .map(ifControl -> ifControl.block().instructionBlock())
            .map(symbolAndScopeManagement::getRecordedScope)
            .toList();
    allBlocks.addAll(allIfInstructionBlocks);
    return allBlocks;
  }

}
