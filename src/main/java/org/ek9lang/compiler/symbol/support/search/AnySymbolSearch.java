package org.ek9lang.compiler.symbol.support.search;

/**
 * Search for a symbol of any category with the matching name.
 */
public final class AnySymbolSearch extends SymbolSearch {
  public AnySymbolSearch(String name) {
    super(name);
    setSearchType(null);
  }

  @Override
  public AnySymbolSearch clone() {
    var rtn = new AnySymbolSearch(getName());
    cloneIntoSearchSymbol(rtn);
    return rtn;
  }
}
