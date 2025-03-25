package org.ek9lang.compiler.ir;

import java.util.List;
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
