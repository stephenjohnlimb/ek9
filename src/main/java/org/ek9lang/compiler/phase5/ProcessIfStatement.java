package org.ek9lang.compiler.phase5;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.phase3.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Deals with checking if all paths through if/else/else-if/else result in variables being initialised.
 * If they do then the outer variable meta-data can also be marked as initialised.
 */
final class ProcessIfStatement extends TypedSymbolAccess implements Consumer<EK9Parser.IfStatementContext> {
  ProcessIfStatement(final SymbolAndScopeManagement symbolAndScopeManagement,
                     final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final EK9Parser.IfStatementContext ctx) {

    //So lets check to see if a variable was initialised in the first guard (only the first guard though).
    processPossibleFirstIfGuardInitialisation(ctx);

    if (ctx.elseOnlyBlock() == null) {
      //Then only an 'if' or a set of 'ifs' may/may not have set any of the unassigned variables.
      //So we cannot modify the outer scope variables, so we're done
      return;
    }


    //And these are all the if else-if else blocks that have to be checked.
    List<IScope> allIfElseBlocks = getAllBlocks(ctx);

    //Now we have all the instruction blocks - we can check if any of the uninitialised variables in this scope
    //Have actually been initialised in all the blocks - meaning that the variable will now be initialised by every
    //path. If this is the case we can mark the outer variable data as being initialised.

    //This is the outer scope where it may be possible to mark a variable as initialised if all scopes have initialised
    final var outerScope = symbolAndScopeManagement.getTopScope();
    //So these are the variables we need to check to see if we can mark then initialised.
    var unInitialisedVariables = symbolAndScopeManagement.getUninitialisedVariablesInCurrentScope();

    for (var variable : unInitialisedVariables) {
      if (isVariableInitialisedInEveryScope(variable, allIfElseBlocks)) {
        symbolAndScopeManagement.markSymbolAsInitialised(variable, outerScope);
      }
    }

  }

  /**
   * Now when the first 'if' has a guard it is 'always' executed and so may initialise a variable.l
   */
  private void processPossibleFirstIfGuardInitialisation(final EK9Parser.IfStatementContext ctx) {

    if (ctx.ifControlBlock().get(0).preFlowAndControl() != null
        && ctx.ifControlBlock().get(0).preFlowAndControl().preFlowStatement() != null
        && ctx.ifControlBlock().get(0).preFlowAndControl().preFlowStatement().guardExpression() != null) {

      var variable = symbolAndScopeManagement.getRecordedSymbol(
          ctx.ifControlBlock().get(0).preFlowAndControl().preFlowStatement().guardExpression().identifier());
      var scope = symbolAndScopeManagement.getRecordedScope(ctx);

      boolean initialised = symbolAndScopeManagement.isVariableInitialised(variable, scope);
      if (initialised) {
        final var outerScope = symbolAndScopeManagement.getTopScope();
        symbolAndScopeManagement.markSymbolAsInitialised(variable, outerScope);
      }
    }

  }

  private boolean isVariableInitialisedInEveryScope(final ISymbol variable, final List<IScope> scopes) {

    return scopes.stream()
        .allMatch(scope -> symbolAndScopeManagement.isVariableInitialised(variable, scope));

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
