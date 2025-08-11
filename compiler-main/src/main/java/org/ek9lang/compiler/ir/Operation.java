package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * An operation of some sort on a Construct.
 * <p>
 * For example in the case of normal classes/traits/components etc. this would just be a method.
 * </p>
 * <p>
 * In the case of a function it would also be a method but with a name of 'call' and the normal
 * function signature would be applied (to the 'call' operation).
 * </p>
 */
public final class Operation implements INode {

  private final DebugInfo debugInfo;
  private BasicBlockInstr basicBlockBody;
  private final ISymbol symbol;


  public Operation(final ISymbol symbol, final DebugInfo debugInfo) {
    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;
    this.debugInfo = debugInfo;

  }

  public BasicBlockInstr getBody() {
    return basicBlockBody;
  }

  public void setBody(final BasicBlockInstr basicBlockBody) {
    this.basicBlockBody = basicBlockBody;
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  public DebugInfo getDebugInfo() {
    return debugInfo;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Operation{" +
        "symbol=" + symbol +
        ", body=" + basicBlockBody +
        '}';
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }
}
