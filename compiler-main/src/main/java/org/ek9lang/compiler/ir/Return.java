package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.VariableSymbol;
import org.ek9lang.core.AssertValue;

/**
 * Models a return value from a method or function.
 */
public final class Return implements INode {
  private final VariableSymbol variableSymbol;

  public Return(final VariableSymbol variableSymbol) {
    AssertValue.checkNotNull("VariableSymbol cannot be null", variableSymbol);
    this.variableSymbol = variableSymbol;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  public ISymbol getReturnType() {
    var returnType = variableSymbol.getType();
    AssertValue.checkTrue("ReturnType must be present", returnType.isPresent());
    return returnType.get();
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Return{" +
        "variableSymbol=" + variableSymbol +
        '}';
  }
}
