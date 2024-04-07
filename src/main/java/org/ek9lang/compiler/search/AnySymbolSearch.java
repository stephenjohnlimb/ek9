package org.ek9lang.compiler.search;

/**
 * Search for a symbol of any category with the matching name.
 */
public final class AnySymbolSearch extends SymbolSearch {
  public AnySymbolSearch(final String name) {

    super(name);
    setSearchType(null);

  }

  public AnySymbolSearch(final AnySymbolSearch from) {

    super(from);

  }
}
