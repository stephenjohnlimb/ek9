package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.RuleSupport;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Checks if a symbol has been referenced or not (can be null - no error).
 * This can be useful within blocks - can indicate an error in ek9 developer logic.
 */
final class CheckReferenced extends RuleSupport implements Consumer<ISymbol> {
  CheckReferenced(final SymbolAndScopeManagement symbolAndScopeManagement,
                  final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(ISymbol symbol) {
    //Can be null if ek9 developer code in error.
    if (symbol != null && !symbol.isReferenced()) {
      errorListener.semanticError(symbol.getSourceToken(), "", ErrorListener.SemanticClassification.NOT_REFERENCED);
    }
  }
}
