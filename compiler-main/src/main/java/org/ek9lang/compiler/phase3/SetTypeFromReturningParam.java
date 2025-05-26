package org.ek9lang.compiler.phase3;

import java.util.function.BiConsumer;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Looks at the returning param (if not null) and sets the symbol type as appropriate.
 */
final class SetTypeFromReturningParam extends TypedSymbolAccess
    implements BiConsumer<ISymbol, EK9Parser.ReturningParamContext> {
  SetTypeFromReturningParam(final SymbolsAndScopes symbolsAndScopes,
                            final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final ISymbol symbol, final EK9Parser.ReturningParamContext ctx) {

    if (ctx == null) {
      symbol.setType(symbolsAndScopes.getEk9Types().ek9Void());
      return;
    }

    final var returning = getRecordedAndTypedSymbol(ctx);

    if (returning != null) {
      symbol.setType(returning.getType());
    }

  }
}
