package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.common.ErrorListener.SemanticClassification.NOT_MUTABLE;

import java.util.function.BiConsumer;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Checks if a symbol can be mutated, this is nothing to do with a pure context.
 * This is just to do with it being a constant of an enumeration value for example.
 * These things can never be altered.
 */
class CheckMutableOrError implements BiConsumer<IToken, ISymbol> {

  private final ErrorListener errorListener;

  CheckMutableOrError(final ErrorListener errorListener) {
    this.errorListener = errorListener;
  }

  @Override
  public void accept(IToken locationForError, ISymbol symbol) {
    if (!symbol.isMutable()) {
      errorListener.semanticError(locationForError, "'" + symbol.getFriendlyName() + "':", NOT_MUTABLE);
    }
  }
}
