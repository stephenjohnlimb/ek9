package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;
import org.ek9lang.compiler.search.SymbolSearch;

/**
 * Used when duplicate symbols are found, where we need to add a placeholder.
 * So the result of the listening and processing can take place, symbols can still be
 * put into some sort of scope; so they don't mess up other valid scopes.
 * The phase will fail compilation with duplicate errors, but we will have ensured that
 * other symbols and processing has been put somewhere.
 */
public class StackConsistencyScope extends ScopedSymbol implements ICanCaptureVariables {

  @Serial
  private static final long serialVersionUID = 1L;

  private CaptureScope capturedVariables;

  public StackConsistencyScope(final IScope enclosingScope, final ScopeType scopeType) {

    super("StackConsistencyScope", enclosingScope);
    super.setScopeType(scopeType);

  }

  public StackConsistencyScope(final IScope enclosingScope) {

    super("StackConsistencyScope", enclosingScope);

  }

  @Override
  public Optional<CaptureScope> getCapturedVariables() {

    return Optional.ofNullable(capturedVariables);
  }

  @Override
  public void setCapturedVariables(CaptureScope capturedVariables) {

    this.capturedVariables = capturedVariables;

  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(final SymbolSearch search) {

    if (getCapturedVariables().isPresent()) {
      final var rtn = getCapturedVariables().get().resolveInThisScopeOnly(search);
      if (rtn.isPresent()) {
        return rtn;
      }
    }

    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolveExcludingCapturedVariables(final SymbolSearch search) {

    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolve(final SymbolSearch search) {

    final var rtn = this.resolveInThisScopeOnly(search);
    if (rtn.isPresent()) {
      return rtn;
    }

    return resolveWithParentScope(search);
  }

  @Override
  public StackConsistencyScope clone(final IScope withParentAsAppropriate) {

    return cloneIntoStackConsistencyScope(
        new StackConsistencyScope(withParentAsAppropriate));
  }

  /**
   * Clones the content of this into the new copy.
   */
  public StackConsistencyScope cloneIntoStackConsistencyScope(final StackConsistencyScope newCopy) {

    super.cloneIntoScopeSymbol(newCopy);
    if (getCapturedVariables().isPresent()) {
      final var captured = getCapturedVariables().get();
      final var cloned = captured.clone(newCopy.getEnclosingScope());
      newCopy.setCapturedVariables(cloned);
    }

    //properties set at construction.
    return newCopy;
  }

  @Override
  public boolean equals(final Object o) {

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
