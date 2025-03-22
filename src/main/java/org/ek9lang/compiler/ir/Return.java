package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Models a return value from a method or function.
 */
public class Return implements INode {
  private final VariableSymbol variableSymbol;

  public Return(final VariableSymbol variableSymbol) {
    AssertValue.checkNotNull("VariableSymbol cannot be null", variableSymbol);
    this.variableSymbol = variableSymbol;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Return{" +
        "variableSymbol=" + variableSymbol +
        '}';
  }
}
