package org.ek9lang.compiler.symbol;

import java.util.Optional;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbol.support.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Used to actually hold the captured symbols.
 * Note that the resolution out to the enclosing scope is switched on and off.
 * This switching is based on entering and exiting the 'capture' via the antlr listeners.
 * It is important because on entering the capture scope it must be possible to resolve varaiables
 * in the enclosing Blocks before, so that variables can be captured).
 * But on exiting the capture scope, the resolution must then only be within this scope and must not
 * look outside to the enclosing scope.
 */
public class CaptureScope extends LocalScope {
  private boolean openToEnclosingScope = false;

  public CaptureScope(IScope enclosingScope) {
    super("Capture", enclosingScope);
    super.setScopeType(ScopeType.CAPTURE_BLOCK);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof CaptureScope that)
        && super.equals(o)
        && getScopeType() == that.getScopeType();
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getScopeType().hashCode();
    return result;
  }

  @Override
  public CaptureScope clone(IScope withParentAsAppropriate) {
    return cloneIntoCaptureScope(new CaptureScope(withParentAsAppropriate));
  }

  /**
   * Clones the content of this into the new copy.
   */
  public CaptureScope cloneIntoCaptureScope(CaptureScope newCopy) {
    super.cloneIntoLocalScope(newCopy);
    //properties set at construction.
    return newCopy;
  }

  @Override
  public void define(ISymbol symbol) {
    if (symbol instanceof VariableSymbol variableSymbol) {
      variableSymbol.setAggregatePropertyField(true);
      variableSymbol.setPrivate(true);
    }
    super.define(symbol);
  }

  public void setOpenToEnclosingScope(boolean openToEnclosingScope) {
    this.openToEnclosingScope = openToEnclosingScope;
  }

  @Override
  protected Optional<ISymbol> resolveWithEnclosingScope(SymbolSearch search) {
    if (openToEnclosingScope) {
      return super.resolveWithEnclosingScope(search);
    }
    return Optional.empty();
  }

  @Override
  protected MethodSymbolSearchResult resolveForAllMatchingMethodsInEnclosingScope(MethodSymbolSearch search,
                                                                                  MethodSymbolSearchResult result) {
    if (openToEnclosingScope) {
      return super.resolveForAllMatchingMethodsInEnclosingScope(search, result);
    }
    return result;
  }
}
