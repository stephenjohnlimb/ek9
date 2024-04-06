package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NONE_PURE_CALL_IN_PURE_SCOPE;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Just check that with in the current 'context' - available from 'TypedSymbolAccess'.
 */
final class CheckPureContext extends TypedSymbolAccess implements BiConsumer<IToken, ISymbol> {
  CheckPureContext(final SymbolAndScopeManagement symbolAndScopeManagement,
                   final ErrorListener errorListener) {

    super(symbolAndScopeManagement, errorListener);

  }

  @Override
  public void accept(final IToken accessLocationToken, final ISymbol symbol) {

    //Order here is important do the easiest lowest effort processing first.
    if (!symbol.isMarkedPure() && isProcessingScopePure()) {
      errorListener.semanticError(accessLocationToken, "'" + symbol.getFriendlyName() + "':",
          NONE_PURE_CALL_IN_PURE_SCOPE);
    }

  }
}
