package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_MUTABLE;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.symbols.IMayReturnSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks if a symbol can be mutated, this is nothing to do with a pure context.
 * This is just to do with it being a constant of an enumeration value for example.
 * These things can never be altered.
 */
final class CheckMutableOrError implements BiConsumer<IToken, ISymbol> {

  final SymbolAndScopeManagement symbolAndScopeManagement;
  private final ErrorListener errorListener;

  CheckMutableOrError(final SymbolAndScopeManagement symbolAndScopeManagement,
                      final ErrorListener errorListener) {
    this.symbolAndScopeManagement = symbolAndScopeManagement;
    this.errorListener = errorListener;

  }

  @Override
  public void accept(final IToken locationForError, final ISymbol symbol) {

    if (symbol instanceof IMayReturnSymbol functionOrMethod
        && functionOrMethod.isReturningSymbolPresent()) {
      emitMutableOrError(locationForError, functionOrMethod.getReturningSymbol());
    } else {
      emitMutableOrError(locationForError, symbol);
    }

  }

  private void emitMutableOrError(final IToken locationForError, final ISymbol symbol) {
    if (!symbol.isMutable()) {
      errorListener.semanticError(locationForError, "'" + symbol.getFriendlyName() + "':", NOT_MUTABLE);
    }

    symbol.getType().ifPresent(symbolType -> {
      if (symbolAndScopeManagement.getEk9Types().ek9Void().isExactSameType(symbolType)) {
        errorListener.semanticError(locationForError, "'Void' type:", NOT_MUTABLE);
      }
    });
  }
}
