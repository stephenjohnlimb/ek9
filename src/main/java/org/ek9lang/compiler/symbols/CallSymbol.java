package org.ek9lang.compiler.symbols;

import java.io.Serial;
import org.ek9lang.compiler.support.ReturnTypeExtractor;

/**
 * Just re-uses the bulk of method symbol for when we want to make a symbol that is a call
 * to an actual method.
 * This will be used to build up the sort of call we want to make based on the source .
 * We then have to resolve this CallSymbol against a real method symbol.
 */
public class CallSymbol extends MethodSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  private ScopedSymbol resolvedSymbolToCall = null;

  /**
   * Now dynamic functions are both calls at creation and calls when 'called'.
   * So while I hate 'exceptions and one offs', functions are both a type and a thing to be called.
   * We have to ensure we distinguish between the two.
   */
  private boolean formOfDeclarationCall = false;

  public CallSymbol(final String name, final IScope enclosingScope) {

    super(name, enclosingScope);

  }

  public boolean isFormOfDeclarationCall() {

    return formOfDeclarationCall;
  }

  public void setFormOfDeclarationCall(final boolean formOfDeclarationCall) {

    this.formOfDeclarationCall = formOfDeclarationCall;

  }

  @Override
  public CallSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoCallSymbol(new CallSymbol(getName(), withParentAsAppropriate));
  }

  protected CallSymbol cloneIntoCallSymbol(final CallSymbol newCopy) {

    super.cloneIntoMethodSymbol(newCopy);
    newCopy.resolvedSymbolToCall = resolvedSymbolToCall;
    newCopy.setFormOfDeclarationCall(this.isFormOfDeclarationCall());

    return newCopy;
  }

  public ScopedSymbol getResolvedSymbolToCall() {

    return resolvedSymbolToCall;
  }


  /**
   * Set the actual method/function that should be called.
   */
  public void setResolvedSymbolToCall(final ScopedSymbol symbol) {
    final var returnTypeExtractor = new ReturnTypeExtractor(isFormOfDeclarationCall());

    this.resolvedSymbolToCall = symbol;
    this.setType(returnTypeExtractor.apply(symbol));

    //make a note if this method ia actually an operator.
    if (symbol instanceof MethodSymbol method) {
      this.setOperator(method.isOperator());
      this.setMarkedPure(method.isMarkedPure());
    } else if (symbol instanceof IAggregateSymbol aggregate) {
      //If one constructor is marked pure then they are all pure
      this.setMarkedPure(aggregate.getConstructors().get(0).isMarkedPure());
    } else if (symbol instanceof FunctionSymbol function) {
      this.setMarkedPure(function.isMarkedPure());
    }

  }

  @Override
  public boolean isReturningSymbolPresent() {
    if (resolvedSymbolToCall instanceof IMayReturnSymbol canReturn) {
      return canReturn.isReturningSymbolPresent();
    }
    return false;
  }

  @Override
  public ISymbol getReturningSymbol() {
    if (resolvedSymbolToCall instanceof IMayReturnSymbol canReturn) {
      return canReturn.getReturningSymbol();
    }
    return null;
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

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }

    if ((o instanceof CallSymbol that) && super.equals(o)) {
      return getResolvedSymbolToCall() != null ? getResolvedSymbolToCall().equals(that.getResolvedSymbolToCall()) :
          that.getResolvedSymbolToCall() == null;
    }

    return false;
  }

  @Override
  public int hashCode() {

    int result = super.hashCode();
    result = 31 * result + (getSourceToken() != null ? getSourceToken().hashCode() : 0);
    result = 31 * result + (getResolvedSymbolToCall() != null ? getResolvedSymbolToCall().hashCode() : 0);

    return result;
  }
}
