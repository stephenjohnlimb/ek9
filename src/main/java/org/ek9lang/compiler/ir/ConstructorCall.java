package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Specific Marker node for calling a constructor.
 * This typically involves processing and is a significant activity,
 * rather than just a method or function call.
 */
public final class ConstructorCall extends Call {

  public ConstructorCall(final CallSymbol callSymbol, final MethodSymbol methodSymbol, final List<Argument> arguments) {
    super(callSymbol, methodSymbol, arguments);
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Call{" +
        "callSymbol=" + getCallSymbol() +
        ", methodSymbol=" + getMethodSymbol() +
        ", arguments=" + getArguments() +
        '}';
  }
}
