package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * Not really an item that has executable qualities, but more as a marker in code.
 * This can typically be used when implementing loops and GOTO's in Java byte code or LLVM.
 */
@SuppressWarnings("java:S6206")
public final class Marker implements INode {
  private final String name;

  public Marker(final String name) {
    AssertValue.checkNotNull("Name cannot be null", name);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Marker{" +
        "name='" + name + '\'' +
        '}';
  }
}
