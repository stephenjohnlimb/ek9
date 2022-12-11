package org.ek9lang.compiler.symbol.support.search;

import org.ek9lang.compiler.symbol.ISymbol;

/**
 * A Search for a concrete actual TYPE like a RECORD or a CLASS for example.
 * But this would not search for templated types like 'List of String' i.e. when templated.
 */
public final class TypeSymbolSearch extends SymbolSearch {

  public TypeSymbolSearch(String name) {
    super(name);
    setSearchType(ISymbol.SymbolCategory.TYPE);
  }

  public TypeSymbolSearch(TypeSymbolSearch from) {
    super(from);
  }

}
