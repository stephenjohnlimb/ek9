package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;
import org.ek9lang.compiler.search.SymbolSearch;
import org.ek9lang.compiler.support.ToCommaSeparated;

/**
 * Just focuses on the ability to possible capture variables from an enclosing scope.
 * This is primarily used for dynamic functions and classes.
 */
public class CaptureScopedSymbol extends ScopedSymbol implements ICanCaptureVariables {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * This is the module this function has been defined in.
   */
  private IScope moduleScope;

  /**
   * Was it marked abstract in the source code.
   */
  private boolean markedAbstract = false;

  /**
   * For dynamic functions/types we can capture variables from the enclosing scope(s) and pull them in.
   * We can then hold and access them in the dynamic function/type even when the function has moved
   * out of the original scope. i.e. a sort of closure over variables.
   */
  private CaptureScope capturedVariables;


  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public CaptureScopedSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
    super(name, type, enclosingScope);
  }

  public CaptureScopedSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
  }

  @Override
  public ScopedSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoCaptureScopedSymbol(new CaptureScopedSymbol(this.getName(), withParentAsAppropriate));
  }

  protected CaptureScopedSymbol cloneIntoCaptureScopedSymbol(CaptureScopedSymbol newCopy) {
    super.cloneIntoScopeSymbol(newCopy);
    newCopy.moduleScope = moduleScope;
    newCopy.markedAbstract = markedAbstract;

    if (getCapturedVariables().isPresent()) {
      var captured = getCapturedVariables().get();
      var cloned = captured.clone(newCopy.getEnclosingScope());
      newCopy.setCapturedVariables(cloned);
    }

    return newCopy;
  }

  public IScope getModuleScope() {
    return moduleScope;
  }

  public void setModuleScope(IScope moduleScope) {
    this.moduleScope = moduleScope;
  }

  @Override
  public boolean isMarkedAbstract() {
    return markedAbstract;
  }

  public void setMarkedAbstract(boolean markedAbstract) {
    this.markedAbstract = markedAbstract;
  }

  public Optional<CaptureScope> getCapturedVariables() {
    return Optional.ofNullable(capturedVariables);
  }

  /**
   * It is possible to capture variables in the current scope and pull them into the
   * function, so they can be used.
   */
  public void setCapturedVariables(CaptureScope capturedVariables) {
    setCapturedVariables(Optional.ofNullable(capturedVariables));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setCapturedVariables(Optional<CaptureScope> capturedVariables) {
    capturedVariables.ifPresentOrElse(vars -> this.capturedVariables = vars, () -> this.capturedVariables = null);

  }

  /**
   * The variables that have been captured can be given public access if needed.
   */
  public void setCapturedVariablesVisibility(final boolean isPublic) {
    getCapturedVariables().ifPresent(
        localScope -> localScope.getSymbolsForThisScope().forEach(symbol -> {
          if (symbol instanceof VariableSymbol s) {
            s.setPrivate(!isPublic);
          }
        }));
  }

  protected String getPrivateVariablesForDisplay() {
    var toCommaSeparated = new ToCommaSeparated(this, true);
    return getCapturedVariables()
        .map(IScope::getSymbolsForThisScope)
        .map(toCommaSeparated)
        .orElse("");
  }

  @Override
  public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search) {
    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
    if (rtn.isEmpty() && getCapturedVariables().isPresent()) {
      rtn = getCapturedVariables().get().resolveInThisScopeOnly(search);
    }
    return rtn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    return (o instanceof CaptureScopedSymbol that)
        && super.equals(o)
        && isMarkedAbstract() == that.isMarkedAbstract()
        && getCapturedVariables().equals(that.getCapturedVariables());
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (getSourceToken() != null ? getSourceToken().hashCode() : 0);
    result = 31 * result + (isMarkedAbstract() ? 1 : 0);
    result = 31 * result + getCapturedVariables().hashCode();
    return result;
  }
}
