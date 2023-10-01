package org.ek9lang.compiler.symbols;

import java.io.Serial;
import java.util.Optional;

/**
 * Scope for callable methods (operations) that are part of a Service.
 */
public class ServiceOperationSymbol extends MethodSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public ServiceOperationSymbol(String name, Optional<ISymbol> type, IScope enclosingScope) {
    super(name, type, enclosingScope);
  }

  public ServiceOperationSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
  }

  @Override
  public ServiceOperationSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoServiceOperationSymbol(
        new ServiceOperationSymbol(getName(), getType(), withParentAsAppropriate));
  }

  protected ServiceOperationSymbol cloneIntoServiceOperationSymbol(ServiceOperationSymbol newCopy) {
    super.cloneIntoMethodSymbol(newCopy);
    return newCopy;
  }
}
