package org.ek9lang.compiler.symbol.support.search;

import java.util.List;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Search for a symbol of any types category with the matching name.
 * i.e. TYPE, TEMPLATE_TYPE, FUNCTION and FUNCTION_TYPE.
 */
public final class AnyTypeSymbolSearch extends SymbolSearch {

  /**
   * Constructor for any 'type' search.
   */
  public AnyTypeSymbolSearch(String name) {
    super(name);
    setSearchType(null);
    setVetoSearchTypes(List.of(
        ISymbol.SymbolCategory.METHOD,
        ISymbol.SymbolCategory.VARIABLE,
        ISymbol.SymbolCategory.CONTROL));
  }
}
