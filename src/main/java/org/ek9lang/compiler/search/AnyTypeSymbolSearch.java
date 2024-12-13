package org.ek9lang.compiler.search;

import java.util.List;
import org.ek9lang.compiler.symbols.SymbolCategory;

/**
 * Search for a symbol of any types category with the matching name.
 * i.e. TYPE, TEMPLATE_TYPE, FUNCTION and FUNCTION_TYPE.
 */
public final class AnyTypeSymbolSearch extends SymbolSearch {

  /**
   * Constructor for any 'type' search.
   */
  public AnyTypeSymbolSearch(final String name) {

    super(name);
    setSearchType(null);
    setVetoSearchTypes(List.of(
        SymbolCategory.METHOD,
        SymbolCategory.VARIABLE,
        SymbolCategory.CONTROL));

  }
}
