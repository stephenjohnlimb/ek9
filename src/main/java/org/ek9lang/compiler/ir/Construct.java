package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;

/**
 * In EK9 every construct becomes one of these nodes.
 * So that means class, trait, component, program, function - everything.
 * <p>
 * In the case of functions there is a single 'call' method, this has all the
 * parameters that would be supplied when other constructs use the function.
 * </p>
 * <p>
 * So this is more like a Function in Java - that is really just an object.
 * The reason for this is that in EK9 it is possible for functions to 'capture' variables
 * as properties (dynamic functions). But this whole mechanism makes sense even for normal
 * fixed named functions. It also means it is very easy to pass functions around as data.
 * </p>
 */
public final class Construct implements INode {

  private final ISymbol symbol;

  private final List<Operation> operations = new ArrayList<>();

  public Construct(final ISymbol symbol) {

    AssertValue.checkNotNull("Symbol cannot be null", symbol);
    this.symbol = symbol;

  }

  public String getFullyQualifiedName() {
    return symbol.getFullyQualifiedName();
  }

  /**
   * Assess if this construct is a function or a general aggregate symbol type.
   *
   * @return true if the construct is just a function - otherwise false and it is an aggregate.
   */
  public boolean isFunction() {
    return symbol.isFunction();
  }

  public void add(final Operation operation) {
    AssertValue.checkNotNull("Operation cannot be null", operation);
    operations.add(operation);
  }

  public ISymbol getSymbol() {
    return symbol;
  }

  public List<Operation> getOperations() {
    return List.copyOf(operations);
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Construct{" +
        "symbol=" + symbol +
        ", operations=" + operations +
        '}';
  }
}
