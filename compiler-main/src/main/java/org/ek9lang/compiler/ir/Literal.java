package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Represents a literal value.
 */
@SuppressWarnings("java:S6206")
public final class Literal implements INode {
  private final ISymbol symbol;

  public Literal(final ISymbol symbol) {
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Literal{" +
        "symbol=" + symbol +
        '}';
  }
}
