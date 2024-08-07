package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if a symbol has been referenced or not (can be null - no error).
 * This can be useful within blocks - can indicate an error in ek9 developer logic.
 */
final class SymbolReferencedOrError extends TypedSymbolAccess implements Consumer<ISymbol> {
  SymbolReferencedOrError(final SymbolsAndScopes symbolsAndScopes,
                          final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final ISymbol symbol) {

    //Can be null if ek9 developer's code is in error.
    if (symbol != null && !symbol.isReferenced()) {
      errorListener.semanticError(symbol.getSourceToken(), "", ErrorListener.SemanticClassification.NOT_REFERENCED);
    }

  }
}
