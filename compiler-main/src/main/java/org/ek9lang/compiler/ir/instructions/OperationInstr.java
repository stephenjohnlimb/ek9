package org.ek9lang.compiler.ir.instructions;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * An operation on some sort on a Construct.
 * By this we are talking about methods and operators, but also constructors and 'init' operations.
 * <p>
 * For example in the case of normal classes/traits/components etc. this would just be a method.
 * </p>
 * <p>
 * In the case of a function it would also be a method but with a name of 'call' and the normal
 * function signature would be applied (to the 'call' operation).
 * </p>
 */
public final class OperationInstr implements INode, Comparable<OperationInstr> {

  private final DebugInfo debugInfo;
  private BasicBlockInstr basicBlockBody;
  private final ISymbol symbol;
  private boolean isDispatcher = false;
  private final List<OperationInstr> dispatchImplementations = new ArrayList<>();

  public OperationInstr(final ISymbol symbol, final DebugInfo debugInfo) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;
    this.debugInfo = debugInfo;

  }

  public BasicBlockInstr getBody() {
    return basicBlockBody;
  }

  public OperationInstr setBody(final BasicBlockInstr basicBlockBody) {
    this.basicBlockBody = basicBlockBody;
    return this;
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  public DebugInfo getDebugInfo() {
    return debugInfo;
  }

  public boolean isDispatcher() {
    return isDispatcher;
  }

  public void setDispatcher(boolean dispatcher) {
    isDispatcher = dispatcher;
  }

  public List<OperationInstr> getDispatchImplementations() {
    return List.copyOf(dispatchImplementations);
  }

  public void addDispatchImplementation(final OperationInstr impl) {
    AssertValue.checkNotNull("Dispatch implementation cannot be null", impl);
    dispatchImplementations.add(impl);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "OperationInstr{"
        + "symbol=" + symbol
        + ", isDispatcher=" + isDispatcher
        + ", dispatchImplementations=" + dispatchImplementations
        + ", body=" + basicBlockBody
        + '}';
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public int compareTo(final OperationInstr o) {
    return symbol.getFriendlyName().compareTo(o.getSymbol().getFriendlyName());
  }
}
