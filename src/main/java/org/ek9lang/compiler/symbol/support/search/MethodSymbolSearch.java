package org.ek9lang.compiler.symbol.support.search;

import java.util.Optional;
import org.ek9lang.compiler.symbol.ISymbol;
import org.ek9lang.compiler.symbol.MethodSymbol;

/**
 * Quite a few option to a method search.
 * In some cases you need to be exact but in others you
 * want the return type left open.
 * So there are multiple constructors to support this.
 */
public final class MethodSymbolSearch extends SymbolSearch {
  public MethodSymbolSearch(SymbolSearch from) {
    this(from.getName(), from);
  }

  /**
   * Create a method search from an existing search, but with a new name.
   */
  public MethodSymbolSearch(String newName, SymbolSearch from) {
    this(newName);
    setParameters(from.getParameters());
    if (from.getOfTypeOrReturn().isPresent()) {
      this.setOfTypeOrReturn(from.getOfTypeOrReturn());
    }
  }

  /**
   * Create a method search given a method symbol.
   * But this does not set the expected type.
   * This is very useful for finding overridden methods where the returning type is different
   * but compatible.
   */
  public MethodSymbolSearch(MethodSymbol methodSymbol) {
    this(methodSymbol.getName());
    this.setParameters(methodSymbol.getSymbolsForThisScope());
    //don't set the return type leave that open, so we can handle covariance.
  }

  public MethodSymbolSearch(String name) {
    super(name);
    setSearchType(ISymbol.SymbolCategory.METHOD);
  }

  public MethodSymbolSearch(String name, Optional<ISymbol> ofTypeOrReturn) {
    super(name, ofTypeOrReturn);
    setSearchType(ISymbol.SymbolCategory.METHOD);
  }

  public MethodSymbolSearch(String name, ISymbol ofTypeOrReturn) {
    super(name, ofTypeOrReturn);
    setSearchType(ISymbol.SymbolCategory.METHOD);
  }

  @Override
  public MethodSymbolSearch clone() {
    var rtn = new MethodSymbolSearch(getName());
    cloneIntoSearchSymbol(rtn);
    return rtn;
  }

  @Override
  public MethodSymbolSearch setOfTypeOrReturn(Optional<ISymbol> ofTypeOrReturn) {
    super.setOfTypeOrReturn(ofTypeOrReturn);
    return this;
  }

  @Override
  public MethodSymbolSearch setOfTypeOrReturn(ISymbol ofTypeOrReturn) {
    super.setOfTypeOrReturn(ofTypeOrReturn);
    return this;
  }
}