package org.ek9lang.compiler.phase5;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NEVER_INITIALISED;

import java.util.function.Consumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.ExternallyImplemented;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;

/**
 * Accepts a variable only declaration and ensures the variable is recorded if appropriate, for transient flow checks.
 */
final class VariableOnlyOrError extends TypedSymbolAccess
    implements Consumer<EK9Parser.VariableOnlyDeclarationContext> {

  private final ExternallyImplemented externallyImplemented = new ExternallyImplemented();

  VariableOnlyOrError(final SymbolsAndScopes symbolsAndScopes,
                      final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final EK9Parser.VariableOnlyDeclarationContext ctx) {

    if (ctx.QUESTION() != null
        && symbolsAndScopes.getRecordedSymbol(ctx) instanceof VariableSymbol variable) {
      //Then we know it was not initialised at declaration, so record it,
      //this will be recorded against the current scope
      symbolsAndScopes.recordSymbolDeclaration(variable);
      propertyInitialisedOrError(variable);
    }

  }

  /**
   * If this variable is actually a property on some type of aggregate check if it has ever been initialised.
   * If not then that's an error.
   */
  private void propertyInitialisedOrError(final ISymbol variable) {

    //Might be part of an 'extern' module, where the field/property is declared but not assigned.
    //This is fine for an 'extern' declaration as the implementation will have actually dealt with it.
    if (externallyImplemented.test(variable)) {
      return;
    }

    if (variable.isPropertyField() && !variable.isInitialised()) {
      errorListener.semanticError(variable.getSourceToken(), "'" + variable.getFriendlyName() + ":", NEVER_INITIALISED);
    }

  }
}
