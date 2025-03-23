package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * A reference to a variable.
 */
public final class VariableRef implements INode {

  private final ISymbol symbol;

  public VariableRef(final ISymbol symbol) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;

  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "VariableRef{" +
        "symbol=" + symbol +
        '}';
  }
}
