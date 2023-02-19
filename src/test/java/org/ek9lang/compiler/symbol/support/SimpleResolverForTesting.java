package org.ek9lang.compiler.symbol.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;

/**
 * Just used in testing for resolving a symbol.
 */
public class SimpleResolverForTesting implements Function<String, Optional<ISymbol>> {

  private final IScope scopeForResolution;
  private final boolean thisScopeOnly;

  public SimpleResolverForTesting(final IScope scopeForResolution, final boolean thisScopeOnly) {
    this.scopeForResolution = scopeForResolution;
    this.thisScopeOnly = thisScopeOnly;
  }

  @Override
  public Optional<ISymbol> apply(String nameToBeResolved) {
    if (thisScopeOnly) {
      return scopeForResolution.resolveInThisScopeOnly(new AnySymbolSearch(nameToBeResolved));
    }
    return scopeForResolution.resolve(new AnySymbolSearch(nameToBeResolved));
  }
}
