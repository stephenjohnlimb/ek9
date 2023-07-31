package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks that variable only and variable declarations have symbols that have been referenced/initialised.
 */
final class CheckInstructionBlockVariables extends RuleSupport implements Consumer<EK9Parser.InstructionBlockContext> {

  private final Consumer<ISymbol> symbolCheck;

  /**
   * Check on references to variables in blocks.
   */
  CheckInstructionBlockVariables(final SymbolAndScopeManagement symbolAndScopeManagement,
                                 final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);

    final var checkInitialised = new CheckInitialised(symbolAndScopeManagement, errorListener);
    final var checkReferenced = new CheckReferenced(symbolAndScopeManagement, errorListener);
    this.symbolCheck = checkReferenced.andThen(checkInitialised);
  }

  @Override
  public void accept(EK9Parser.InstructionBlockContext ctx) {
    ctx.blockStatement().forEach(blockStatement -> {
      if (blockStatement.variableOnlyDeclaration() != null) {
        symbolCheck.accept(symbolAndScopeManagement.getRecordedSymbol(blockStatement.variableOnlyDeclaration()));
      } else if (blockStatement.variableDeclaration() != null) {
        symbolCheck.accept(symbolAndScopeManagement.getRecordedSymbol(blockStatement.variableDeclaration()));
      }
    });
  }
}
