package org.ek9lang.compiler.symbols;

import java.io.Serial;

/**
 * EK9 switch/try control type symbol - this can effectively return a value if it is configured
 * with returning part.
 * When generating output we need this to create its own block so variables inside are hidden
 * from later scopes.
 * So as we have a returning part (optional) we need a scope to put it in (i.e. this scope)
 * this then means that when we finally come to generate the output - this symbol will be able
 * to supply the outer variable to set the result to.
 */
public class ControlSymbol extends ScopedSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  /**
   * Create a new Control Symbol.
   */
  public ControlSymbol(final String name, final IScope enclosingScope) {

    super(name, enclosingScope);
    super.setCategory(SymbolCategory.CONTROL);
    //So it is a sort of function in a way.
    super.setGenus(SymbolGenus.FUNCTION);

  }

  @Override
  public ControlSymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoControlSymbol(new ControlSymbol(this.getName(), withParentAsAppropriate));
  }

  protected ControlSymbol cloneIntoControlSymbol(final ControlSymbol newCopy) {

    super.cloneIntoScopeSymbol(newCopy);

    return newCopy;
  }

}