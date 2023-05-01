package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Checks that variable only and variable declarations have symbols that have been referenced/initialised.
 */
public class CheckInstructionBlockVariables implements Consumer<EK9Parser.InstructionBlockContext> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ErrorListener errorListener;

  /**
   * Check on references to variables in blocks.
   */
  public CheckInstructionBlockVariables(final SymbolAndScopeManagement symbolAndScopeManagement,
                                        final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;
  }

  @Override
  public void accept(EK9Parser.InstructionBlockContext ctx) {
    ctx.blockStatement().forEach(blockStatement -> {
      if (blockStatement.variableOnlyDeclaration() != null) {
        checkSymbol(symbolAndScopeManagement.getRecordedSymbol(blockStatement.variableOnlyDeclaration()));
      } else if (blockStatement.variableDeclaration() != null) {
        checkSymbol(symbolAndScopeManagement.getRecordedSymbol(blockStatement.variableDeclaration()));
      }
    });
  }

  private void checkSymbol(final ISymbol symbol) {
    checkReferencedOrError(symbol);
    checkInitialisedOrError(symbol);
  }

  private void checkReferencedOrError(final ISymbol symbol) {
    //Can be null if ek9 developer code in error.
    if (symbol != null && !symbol.isReferenced()) {
      errorListener.semanticError(symbol.getSourceToken(), "", ErrorListener.SemanticClassification.NOT_REFERENCED);
    }
  }

  private void checkInitialisedOrError(final ISymbol symbol) {
    //Can be null if ek9 developer code in error.
    if (symbol != null && !symbol.isInitialised()) {
      errorListener.semanticError(symbol.getSourceToken(), "", ErrorListener.SemanticClassification.NEVER_INITIALISED);
    }
  }
}
