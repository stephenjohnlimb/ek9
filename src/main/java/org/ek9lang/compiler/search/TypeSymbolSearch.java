package org.ek9lang.compiler.search;

import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * A Search for a concrete actual TYPE like a RECORD or a CLASS for example.
 * But this would not search for templated types like 'List of String' i.e. when templated.
 */
public final class TypeSymbolSearch extends SymbolSearch {

  public TypeSymbolSearch(final String name) {

    super(name);
    setSearchType(SymbolCategory.TYPE);

  }

  public TypeSymbolSearch(TypeSymbolSearch from) {

    this(from.getName());

  }
}
