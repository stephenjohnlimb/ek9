package org.ek9lang.compiler.phase5;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * It may be that before calling this you have determined that 'isSet' has been called or you 'know' that
 * the value of the variable is present and set. This is particularly useful for Optional and Result types.
 * because it means that if we 'know' the Optional/Result 'isSet' then we can allow the ek9 developer
 * direct access to 'get()' and 'ok()' without any further checks.
 */
final class SafeSymbolMarker extends AbstractSafeSymbolMarker implements BiConsumer<ISymbol, IScope> {

  SafeSymbolMarker(final SymbolsAndScopes symbolsAndScopes, final ErrorListener errorListener) {
    super(symbolsAndScopes, errorListener);
  }

  @Override
  public void accept(final ISymbol symbol, final IScope scope) {
    assessIsSetCall(symbol, scope);
  }
}
