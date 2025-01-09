package org.ek9lang.compiler.phase5;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.PossibleGenericSymbol;

final class HasTypeOfGeneric implements Predicate<ISymbol> {

  private final ISymbol matchType;

  /**
   * Constructor to check if symbol has a type that matches the use of the match generic type.
   */
  HasTypeOfGeneric(final ISymbol matchType) {

    this.matchType = matchType;
  }

  @Override
  public boolean test(ISymbol symbol) {

    return symbol.getType().isPresent()
        && symbol.getType().get() instanceof PossibleGenericSymbol possibleGenericSymbol
        && possibleGenericSymbol.getGenericType().isPresent()
        && matchType.isExactSameType(possibleGenericSymbol.getGenericType().get());

  }

}
