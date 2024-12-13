package org.ek9lang.compiler.search;

import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * Search just for a matching function by name.
 * Not too bad because functions can be scoped in modules
 * but cannot be overloaded. i.e. have multiple method signatures in the same module.
 */
public final class FunctionSymbolSearch extends SymbolSearch {

  public FunctionSymbolSearch(final String name) {

    super(name);
    setSearchType(SymbolCategory.FUNCTION);

  }

  public FunctionSymbolSearch(FunctionSymbolSearch from) {

    this(from.getName());

  }
}
