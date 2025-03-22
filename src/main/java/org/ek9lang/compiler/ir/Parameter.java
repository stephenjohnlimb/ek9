package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Represents a parameter on a method or a function.
 */
public class Parameter implements INode {
  private final ISymbol symbol;

  public Parameter(final ISymbol symbol) {
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Parameter{" +
        "symbol=" + symbol +
        '}';
  }
}
