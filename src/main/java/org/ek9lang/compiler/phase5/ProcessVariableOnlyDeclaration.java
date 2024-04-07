package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NEVER_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Accepts a variable only declaration and ensures the variable is recorded if appropriate, for transient flow checks.
 */
final class ProcessVariableOnlyDeclaration extends TypedSymbolAccess
    implements Consumer<EK9Parser.VariableOnlyDeclarationContext> {
  ProcessVariableOnlyDeclaration(final SymbolAndScopeManagement symbolAndScopeManagement,
                                 final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    if (ctx.QUESTION() != null) {
      final var variable = (VariableSymbol) symbolAndScopeManagement.getRecordedSymbol(ctx);
      //Then we know it was not initialised at declaration, so record it,
      //this will be recorded against the current scope
      symbolAndScopeManagement.recordSymbolDeclaration(variable);
      propertyInitialisedOrError(variable);
    }

  }

  /**
   * If this variable is actually a property on some type of aggregate check if it has ever been initialised.
   * If not then that's an error.
   */
  private void propertyInitialisedOrError(final ISymbol variable) {

    if (variable.isPropertyField() && !variable.isInitialised()) {
      errorListener.semanticError(variable.getSourceToken(), "'" + variable.getFriendlyName() + ":", NEVER_INITIALISED);
    }

  }
}
