package org.ek9lang.compiler.search;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Just uses the name of a method in the search, to get a list of all possible method name.
 */
public class PossibleMatchingMethods implements Function<MethodSearchInScope, List<MethodSymbol>> {
  @Override
  public List<MethodSymbol> apply(final MethodSearchInScope searchOnAggregate) {
    var toSearch = searchOnAggregate.scopeToSearch();
    var nearMatches = toSearch.getAllSymbolsMatchingName(searchOnAggregate.search().getName());
    return nearMatches
        .stream()
        .filter(ISymbol::isMethod)
        .map(MethodSymbol.class::cast)
        .toList();
  }
}
