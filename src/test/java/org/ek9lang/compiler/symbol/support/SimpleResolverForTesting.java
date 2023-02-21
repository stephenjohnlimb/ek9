package org.ek9lang.compiler.symbol.support;

import java.util.Optional;
import java.util.function.Function;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.ModuleScope;
import org.ek9lang.compiler.symbol.support.search.AnySymbolSearch;

/**
 * Just used in testing for resolving a symbol.
 */
public class SimpleResolverForTesting implements Function<String, Optional<ISymbol>> {

  private final ModuleScope scopeForResolution;
  private final boolean thisScopeOnly;

  public SimpleResolverForTesting(final ModuleScope scopeForResolution, final boolean thisScopeOnly) {
    this.scopeForResolution = scopeForResolution;
    this.thisScopeOnly = thisScopeOnly;
  }

  @Override
  public Optional<ISymbol> apply(String nameToBeResolved) {
    if (thisScopeOnly) {
      return scopeForResolution.resolveInThisModuleOnly(new AnySymbolSearch(nameToBeResolved));
    }
    return scopeForResolution.resolve(new AnySymbolSearch(nameToBeResolved));
  }
}
