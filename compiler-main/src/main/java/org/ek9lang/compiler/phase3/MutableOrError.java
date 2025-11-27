package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_MUTABLE;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks if a symbol can be mutated, this is nothing to do with a pure context.
 * This is just to do with it being a constant or an enumeration value for example.
 * These things can never be altered.
 */
final class MutableOrError implements BiConsumer<IToken, ISymbol> {

  final SymbolsAndScopes symbolsAndScopes;
  private final ErrorListener errorListener;

  MutableOrError(final SymbolsAndScopes symbolsAndScopes,
                 final ErrorListener errorListener) {
    this.symbolsAndScopes = symbolsAndScopes;
    this.errorListener = errorListener;

  }

  @Override
  public void accept(final IToken locationForError, final ISymbol symbol) {

    if (symbol instanceof IMayReturnSymbol functionOrMethod
        && functionOrMethod.isReturningSymbolPresent()) {
      emitNotMutableError(locationForError, functionOrMethod.getReturningSymbol());
    } else {
      emitNotMutableError(locationForError, symbol);
    }

  }

  private void emitNotMutableError(final IToken locationForError, final ISymbol symbol) {
    if (!symbol.isMutable()) {
      errorListener.semanticError(locationForError, "'" + symbol.getFriendlyName() + "' cannot be changed:", NOT_MUTABLE);
    }

    symbol.getType().ifPresent(symbolType -> {
      if (symbolsAndScopes.getEk9Types().ek9Void().isExactSameType(symbolType)) {
        errorListener.semanticError(locationForError, "changes to 'Void' type is meaningless:", NOT_MUTABLE);
      }
    });
  }
}
