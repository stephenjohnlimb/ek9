package org.ek9lang.compiler.symbol;

import java.util.Optional;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Used when duplicate symbols are found, where we need to add a placeholder.
 * So the result of the listening and processing can take place, symbols can still be
 * put into some sort of scope; so they don't mess up other valid scopes.
 * The phase will fail compilation with duplicate errors, but we will have ensured that
 * other symbols and processing has been put somewhere.
 */
public class StackConsistencyScope extends LocalScope implements ICanCaptureVariables {

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<IScope> capturedVariables = Optional.empty();

  public StackConsistencyScope(IScope enclosingScope) {
    super("StackConsistencyScope", enclosingScope);
  }

  @Override
  public Optional<IScope> getCapturedVariables() {
    return capturedVariables;
  }

  @Override
  public void setCapturedVariables(IScope capturedVariables) {
    this.capturedVariables = Optional.ofNullable(capturedVariables);
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
    if (capturedVariables.isPresent()) {
      rtn = capturedVariables.get().resolveInThisScopeOnly(search);
    }
    return rtn;
  }

  @Override
  public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search) {
    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public StackConsistencyScope clone(IScope withParentAsAppropriate) {
    return cloneIntoStackConsistencyScope(
        new StackConsistencyScope(withParentAsAppropriate));
  }

  /**
   * Clones the content of this into the new copy.
   */
  public StackConsistencyScope cloneIntoStackConsistencyScope(StackConsistencyScope newCopy) {
    super.cloneIntoLocalScope(newCopy);
    if (capturedVariables.isPresent()) {
      var captured = capturedVariables.get();
      var cloned = captured.clone(newCopy.getEnclosingScope());
      newCopy.setCapturedVariables(cloned);
    }
    //properties set at construction.
    return newCopy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof StackConsistencyScope that)
        && super.equals(o)
        && getCapturedVariables().equals(that.getCapturedVariables());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + getCapturedVariables().hashCode();
    return result;
  }
}
