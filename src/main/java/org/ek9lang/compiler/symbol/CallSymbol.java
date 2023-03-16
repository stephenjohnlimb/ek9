package org.ek9lang.compiler.symbol;

/**
 * Just re-uses the bulk of method symbol for when we want to make a symbol that is a call
 * to an actual method.
 * This will be used to build up the sort of call we want to make based on the source .
 * We then have to resolve this CallSymbol against a real method symbol.
 */
public class CallSymbol extends MethodSymbol implements IAssignableSymbol {
  private ScopedSymbol resolvedSymbolToCall = null;

  public CallSymbol(String name, IScope enclosingScope) {
    super(name, enclosingScope);
  }

  @Override
  public CallSymbol clone(IScope withParentAsAppropriate) {
    return cloneIntoCallSymbol(new CallSymbol(getName(), withParentAsAppropriate));
  }

  protected CallSymbol cloneIntoCallSymbol(CallSymbol newCopy) {
    super.cloneIntoMethodSymbol(newCopy);
    newCopy.resolvedSymbolToCall = resolvedSymbolToCall;
    return newCopy;
  }

  public ScopedSymbol getResolvedSymbolToCall() {
    return resolvedSymbolToCall;
  }

  /**
   * Set the actual method/function that should be called.
   */
  public void setResolvedSymbolToCall(ScopedSymbol symbol) {
    this.resolvedSymbolToCall = symbol;
    //make a note if this method ia actually an operator.
    if (symbol instanceof MethodSymbol method) {
      this.setOperator(method.isOperator());
    }
  }

  @Override
  public String getFriendlyScopeName() {
    return getFriendlyName();
  }

  @Override
  public String getFriendlyName() {
    if (resolvedSymbolToCall == null) {
      return getName();
    }
    return getName() + " => " + resolvedSymbolToCall.getFriendlyName();
  }
}
