package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NONE_PURE_CALL_IN_PURE_SCOPE;

import java.util.function.Consumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Just check that with in the current 'context' - available from 'TypedSymbolAccess'.
 */
public class CheckCallContext extends TypedSymbolAccess implements Consumer<ISymbol> {
  protected CheckCallContext(SymbolAndScopeManagement symbolAndScopeManagement,
                             ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(ISymbol symbol) {
    //Order here is important do the easiest lowest effort processing first.
    if (!symbol.isMarkedPure() && isProcessingScopePure()) {
      errorListener.semanticError(symbol.getSourceToken(), "'" + symbol.getFriendlyName() + "':",
          NONE_PURE_CALL_IN_PURE_SCOPE);
    }
  }
}
