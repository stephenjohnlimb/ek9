package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * General call to function or method (for example), but not a constructor.
 */
public class Call implements INode {
  private final CallSymbol callSymbol;
  private final MethodSymbol methodSymbol;
  private final List<Argument> arguments;

  public Call(final CallSymbol callSymbol, final MethodSymbol methodSymbol, final List<Argument> arguments) {
    this.callSymbol = callSymbol;
    this.methodSymbol = methodSymbol;
    this.arguments = arguments;
  }

  CallSymbol getCallSymbol() {
    return callSymbol;
  }

  MethodSymbol getMethodSymbol() {
    return methodSymbol;
  }

  List<Argument> getArguments() {
    return arguments;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Call{" +
        "callSymbol=" + callSymbol +
        ", methodSymbol=" + methodSymbol +
        ", arguments=" + arguments +
        '}';
  }
}
