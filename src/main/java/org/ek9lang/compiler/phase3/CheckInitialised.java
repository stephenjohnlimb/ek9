package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if a symbol has been initialised or not (can be null - no error).
 */
final class CheckInitialised extends TypedSymbolAccess implements Consumer<ISymbol> {

  CheckInitialised(final SymbolAndScopeManagement symbolAndScopeManagement,
                   final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final ISymbol symbol) {
    //Can be null if ek9 developer code in error.
    if (symbol != null && !symbol.isInitialised()) {
      errorListener.semanticError(symbol.getSourceToken(), "",
          ErrorListener.SemanticClassification.NOT_INITIALISED_BEFORE_USE);
    }
  }
}
