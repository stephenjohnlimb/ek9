package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

/**
 * Provides the PossibleGenericSymbol type of the symbol if it has been typed, or empty.
 * This is because the 'type' can be an aggregate or it can be a function.
 */
final class SymbolTypeOrEmpty implements Function<ISymbol, Optional<PossibleGenericSymbol>> {
  SymbolTypeOrEmpty() {
  }

  @Override
  public Optional<PossibleGenericSymbol> apply(final ISymbol symbol) {
    if (symbol == null) {
      return Optional.empty();
    }

    if (symbol.getType().isPresent() && symbol.getType().get() instanceof PossibleGenericSymbol typeSymbol) {
      return Optional.of(typeSymbol);
    }
    return Optional.empty();
  }
}
