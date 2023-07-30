package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks that variable only and variable declarations have symbols that have been referenced/initialised.
 */
public class CheckInstructionBlockVariables extends RuleSupport implements Consumer<EK9Parser.InstructionBlockContext> {

  private final Consumer<ISymbol> symbolCheck;

  /**
   * Check on references to variables in blocks.
   */
  public CheckInstructionBlockVariables(final SymbolAndScopeManagement symbolAndScopeManagement,
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
