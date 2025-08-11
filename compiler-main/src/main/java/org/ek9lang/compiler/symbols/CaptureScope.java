package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.search.SymbolSearch;

/**
 * Used to actually hold the captured symbols.
 * Note that the resolution out to the enclosing scope is switched on and off.
 * This switching is based on entering and exiting the 'capture' via the antlr listeners.
 * It is important because on entering the capture scope it must be possible to resolve variables
 * in the enclosing Blocks before, so that variables can be captured).
 * But on exiting the capture scope, the resolution must then only be within this scope and must not
 * look outside to the enclosing scope.
 */
public class CaptureScope extends LocalScope {

  @Serial
  private static final long serialVersionUID = 1L;

  private boolean openToEnclosingScope = false;

  public CaptureScope(final IScope enclosingScope) {

    super("Capture", enclosingScope);
    super.setScopeType(ScopeType.CAPTURE_BLOCK);

  }

  @Override
  public boolean equals(final Object o) {

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
  public CaptureScope clone(final IScope withParentAsAppropriate) {

    return cloneIntoCaptureScope(new CaptureScope(withParentAsAppropriate));
  }

  /**
   * Clones the content of this into the new copy.
   */
  public CaptureScope cloneIntoCaptureScope(final CaptureScope newCopy) {

    super.cloneIntoLocalScope(newCopy);
    //properties set at construction.

    return newCopy;
  }

  @Override
  public void define(final ISymbol symbol) {

    if (symbol instanceof VariableSymbol variableSymbol) {
      variableSymbol.setAggregatePropertyField(true);
      variableSymbol.setPrivate(true);
    }
    super.define(symbol);

  }

  public void setOpenToEnclosingScope(final boolean openToEnclosingScope) {

    this.openToEnclosingScope = openToEnclosingScope;

  }

  @Override
  public Optional<ISymbol> resolveWithEnclosingScope(final SymbolSearch search) {

    if (openToEnclosingScope) {
      return super.resolveWithEnclosingScope(search);
    }

    return Optional.empty();
  }

  @Override
  protected MethodSymbolSearchResult resolveMatchingMethodsInEnclosingScope(final MethodSymbolSearch search,
                                                                            final MethodSymbolSearchResult result) {

    if (openToEnclosingScope) {
      return super.resolveMatchingMethodsInEnclosingScope(search, result);
    }

    return result;
  }
}
