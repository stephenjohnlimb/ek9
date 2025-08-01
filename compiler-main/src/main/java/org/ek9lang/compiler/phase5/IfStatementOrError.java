package org.ek9lang.compiler.phase5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.CodeFlowAnalyzer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Deals with checking if all paths through if/else/else-if/else result in variables meeting criteria.
 * If they do then the outer variable meta-data can also be marked as also meeting the criteria.
 * Initially, this was just were variables initialised. But now the introduction of CodeFlowAnalyzers
 * means that several forms of symbol analysis can take place.
 */
final class IfStatementOrError extends TypedSymbolAccess implements Consumer<EK9Parser.IfStatementContext> {
  private final List<CodeFlowAnalyzer> analyzers;

  IfStatementOrError(final SymbolsAndScopes symbolsAndScopes,
                     final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);
    this.analyzers = symbolsAndScopes.getCodeFlowAnalyzers();
  }

  @Override
  public void accept(final EK9Parser.IfStatementContext ctx) {

    if (isGuardExpressionPresent(ctx)) {
      processGuardInitialisation(analyzers, ctx);
    }

    processBlocks(analyzers, ctx);

  }

  private boolean isGuardExpressionPresent(final EK9Parser.IfStatementContext ctx) {

    final var first = ctx.ifControlBlock().getFirst();
    return first.preFlowAndControl() != null
        && first.preFlowAndControl().preFlowStatement() != null
        && first.preFlowAndControl().preFlowStatement().guardExpression() != null;

  }

  private void processGuardInitialisation(final List<CodeFlowAnalyzer> analyzers,
                                          final EK9Parser.IfStatementContext ctx) {

    analyzers.forEach(analyzer -> processGuardInitialisation(analyzer, ctx));

  }

  /**
   * Now when the first 'if' has a guard it is 'always' executed and so may initialise a variable.
   */
  private void processGuardInitialisation(final CodeFlowAnalyzer analyzer,
                                          final EK9Parser.IfStatementContext ctx) {

    final var variable = symbolsAndScopes.getRecordedSymbol(
        ctx.ifControlBlock().getFirst().preFlowAndControl().preFlowStatement().guardExpression().identifier());
    final var scope = symbolsAndScopes.getRecordedScope(ctx);
    final var initialised = analyzer.doesSymbolMeetAcceptableCriteria(variable, scope);

    if (initialised) {
      final var outerScope = symbolsAndScopes.getTopScope();
      analyzer.markSymbolAsMeetingAcceptableCriteria(variable, outerScope);
    }

  }

  private void processBlocks(final List<CodeFlowAnalyzer> analyzers, final EK9Parser.IfStatementContext ctx) {

    if (ctx.elseOnlyBlock() == null) {
      //Then only an 'if' or a set of 'ifs' may/may not have set any of the variable criteria.
      //So we cannot modify the outer scope variables, so we're done
      return;
    }

    //And these are all the if else-if else blocks that have to be checked.
    final List<IScope> allIfElseBlocks = getAllBlocks(ctx);
    //This is the outer scope where it may be possible to mark a variable as meeting criteria
    final var outerScope = symbolsAndScopes.getTopScope();

    //So these are the variables we need to check to see if we can mark them as meeting the criteria.
    analyzers.forEach(analyzer -> pullUpAcceptableCriteriaToHigherScope(analyzer, allIfElseBlocks, outerScope));

  }

  private void pullUpAcceptableCriteriaToHigherScope(final CodeFlowAnalyzer analyzer,
                                                     final List<IScope> allIfElseBlocks,
                                                     final IScope outerScope) {

    final var unInitialisedVariables = analyzer.getSymbolsNotMeetingAcceptableCriteria(outerScope);

    for (var variable : unInitialisedVariables) {
      if (isVariableInitialisedInEveryScope(analyzer, variable, allIfElseBlocks)) {
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

    final List<IScope> allBlocks = new ArrayList<>();
    allBlocks.add(symbolsAndScopes.getRecordedScope(ctx.elseOnlyBlock().block().instructionBlock()));

    final var allIfInstructionBlocks =
        ctx.ifControlBlock().stream()
            .map(ifControl -> ifControl.block().instructionBlock())
            .map(symbolsAndScopes::getRecordedScope)
            .toList();

    allBlocks.addAll(allIfInstructionBlocks);

    return allBlocks;
  }

}
