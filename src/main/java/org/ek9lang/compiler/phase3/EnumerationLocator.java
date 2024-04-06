package org.ek9lang.compiler.phase3;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.TypeSymbolSearch;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Attempts to locate an Enumeration by name.
 */
final class EnumerationLocator implements Function<String, Optional<ISymbol>> {

  private final SymbolAndScopeManagement symbolAndScopeManagement;

  EnumerationLocator(final SymbolAndScopeManagement symbolAndScopeManagement) {

    this.symbolAndScopeManagement = symbolAndScopeManagement;

  }

  @Override
  public Optional<ISymbol> apply(final String toResolve) {

    final var resolved = symbolAndScopeManagement.getTopScope().resolve(new TypeSymbolSearch(toResolve));
    if (resolved.isPresent() && resolved.get() instanceof IAggregateSymbol aggregate
        && aggregate.getGenus().equals(ISymbol.SymbolGenus.CLASS_ENUMERATION)) {
      return Optional.of(aggregate);
    }

    return Optional.empty();
  }
}
