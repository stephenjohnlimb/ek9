package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Represents an argument to a call on a method or a function.
 * Basically the same as a Parameter in every way.
 * But decided to duplicate just so it's really obvious that 'Parameters'
 * are defined as part of the function/method signature and 'Arguments' are
 * types values that are passed to the function/method where the 'Arguments'
 * match the 'Parameters'.
 * All that checking and resolution is done in the 'front-end' and so at this
 * point we are guaranteed a match (though there maybe promotions of types required).
 */
public final class Argument implements INode {
  private final ISymbol symbol;

  public Argument(final ISymbol symbol) {
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Argument{" +
        "symbol=" + symbol +
        '}';
  }
}
