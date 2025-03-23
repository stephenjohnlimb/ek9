package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * Just the declaration of a variable.
 */
public final class VariableDecl implements INode {

  private final ISymbol symbol;

  public VariableDecl(final ISymbol symbol) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;

  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "VariableDecl{" +
        "symbol=" + symbol +
        '}';
  }
}
