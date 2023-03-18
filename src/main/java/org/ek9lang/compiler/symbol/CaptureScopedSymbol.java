package org.ek9lang.compiler.symbol;

import java.util.Objects;
import java.util.Optional;
import org.ek9lang.compiler.symbol.support.CommonParameterisedTypeDetails;
import org.ek9lang.compiler.symbol.support.search.SymbolSearch;

/**
 * Just focuses on the ability to possible capture variables from an enclosing scope.
 * This is primarily used for dynamic functions and classes.
 */
public class CaptureScopedSymbol extends ScopedSymbol implements ICanCaptureVariables {

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
  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  private Optional<LocalScope> capturedVariables = Optional.empty();


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

    if (capturedVariables.isPresent()) {
      LocalScope newCaptureScope = new LocalScope("CaptureScope", getEnclosingScope());
      capturedVariables.get().cloneIntoLocalScope(newCaptureScope);
      newCopy.setCapturedVariables(newCaptureScope);
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

  public Optional<LocalScope> getCapturedVariables() {
    return capturedVariables;
  }

  /**
   * It is possible to capture variables in the current scope and pull them into the
   * function, so they can be used.
   */
  public void setCapturedVariables(LocalScope capturedVariables) {
    setCapturedVariables(Optional.ofNullable(capturedVariables));
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public void setCapturedVariables(Optional<LocalScope> capturedVariables) {
    this.capturedVariables = capturedVariables;
  }

  /**
   * The variables that have been captured can be given public access if needed.
   */
  public void setCapturedVariablesVisibility(final boolean isPublic) {
    capturedVariables.ifPresent(
        localScope -> localScope.getSymbolsForThisScope().forEach(symbol -> {
          if (symbol instanceof VariableSymbol s) {
            s.setPrivate(!isPublic);
          }
        }));
  }

  protected String getPrivateVariablesForDisplay() {
    return capturedVariables
        .map(scope -> CommonParameterisedTypeDetails.asCommaSeparated(scope.getSymbolsForThisScope(), true))
        .orElse("");
  }

  @Override
  public Optional<ISymbol> resolveExcludingCapturedVariables(SymbolSearch search) {
    return super.resolveInThisScopeOnly(search);
  }

  @Override
  public Optional<ISymbol> resolveInThisScopeOnly(SymbolSearch search) {
    Optional<ISymbol> rtn = super.resolveInThisScopeOnly(search);
    if (rtn.isEmpty() && capturedVariables.isPresent()) {
      rtn = capturedVariables.get().resolveInThisScopeOnly(search);
    }
    return rtn;
  }

  @Override
  public int hashCode() {
    int captureHash = capturedVariables.map(LocalScope::hashCode).orElse(0);
    return Objects.hash(super.hashCode(), captureHash, Boolean.hashCode(markedAbstract));
  }

  @Override
  public boolean equals(Object obj) {
    var rtn = super.equals(obj);
    if (rtn && obj instanceof CaptureScopedSymbol symbol) {
      rtn = markedAbstract == symbol.markedAbstract;
      if (capturedVariables.isPresent() && symbol.capturedVariables.isPresent()) {
        rtn &= capturedVariables.get().equals(symbol.capturedVariables.get());
      } else {
        rtn &= capturedVariables.isEmpty() && symbol.capturedVariables.isEmpty();
      }
    } else {
      rtn = false;
    }
    return rtn;
  }
}