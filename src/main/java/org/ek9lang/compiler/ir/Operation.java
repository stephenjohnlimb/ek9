package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.symbols.IFunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

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

  private final Return returnValue;
  private final List<Parameter> parameters = new ArrayList<>();
  private Block body;
  private final ISymbol symbol;

  public Operation(final ISymbol symbol) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;

    if (symbol instanceof MethodSymbol methodSymbol) {
      processParameters(methodSymbol.getCallParameters());
      this.returnValue = new Return(methodSymbol.getReturningSymbol());
    } else if (symbol instanceof IFunctionSymbol functionSymbol) {
      processParameters(functionSymbol.getCallParameters());
      this.returnValue = new Return(functionSymbol.getReturningSymbol());
    } else {
      throw new CompilerException("Operation can only support functions and methods");
    }
  }

  private void processParameters(final List<ISymbol> callParameters) {
    callParameters.forEach(parameter -> parameters.add(new Parameter(parameter)));
  }

  public String getOperationName() {
    if (symbol.isFunction()) {
      return "_call";
    }

    return symbol.getName();
  }

  public Return getReturn() {
    return returnValue;
  }

  public List<Parameter> getParameters() {
    return List.copyOf(parameters);
  }

  public Block getBody() {
    return body;
  }

  public void setBody(final Block body) {
    this.body = body;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Operation{" +
        "symbol=" + symbol +
        ", parameters=" + parameters +
        ", body=" + body +
        ", returnValue=" + returnValue +
        '}';
  }
}
