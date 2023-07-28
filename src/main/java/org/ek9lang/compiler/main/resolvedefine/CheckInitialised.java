package org.ek9lang.compiler.main.resolvedefine;

import java.util.function.Consumer;
import org.ek9lang.compiler.errors.ErrorListener;
import org.ek9lang.compiler.main.phases.definition.SymbolAndScopeManagement;
import org.ek9lang.compiler.support.RuleSupport;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Checks if a symbol has been initialised or not (can be null - no error).
 */
public class CheckInitialised extends RuleSupport implements Consumer<ISymbol> {

  public CheckInitialised(final SymbolAndScopeManagement symbolAndScopeManagement,
                          final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(ISymbol symbol) {
    //Can be null if ek9 developer code in error.
    if (symbol != null && !symbol.isInitialised()) {
      errorListener.semanticError(symbol.getSourceToken(), "", ErrorListener.SemanticClassification.NEVER_INITIALISED);
    }
  }
}
