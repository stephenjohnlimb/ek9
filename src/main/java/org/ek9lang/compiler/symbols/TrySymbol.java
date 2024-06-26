package org.ek9lang.compiler.symbols;

import java.io.Serial;

/**
 * EK9 try statement - this can effectively return a value if it is configured with returning part.
 * When generating out put we need this to create its own block so variables inside are hidden
 * from later scopes.
 * So as we have a returning part (optional) we need a scope to put it in (i.e. this scope) this
 * then means that when we use the catch and finally parts of the try we can find that returning
 * variable.
 */
public class TrySymbol extends ControlSymbol {

  @Serial
  private static final long serialVersionUID = 1L;

  public TrySymbol(final IScope enclosingScope) {

    super("Try", enclosingScope);

  }

  @Override
  public TrySymbol clone(final IScope withParentAsAppropriate) {

    return cloneIntoTrySymbol(new TrySymbol(withParentAsAppropriate));
  }

  protected TrySymbol cloneIntoTrySymbol(final TrySymbol newCopy) {

    super.cloneIntoControlSymbol(newCopy);

    return newCopy;
  }

}
