package org.ek9lang.compiler.ir;

import org.ek9lang.core.AssertValue;

/**
 * Not really an item that has executable qualities, but more as a marker in code.
 * This can typically be used when implementing loops and GOTO's in Java byte code or LLVM.
 */
public class Label implements INode {
  private final String name;

  public Label(final String name) {
    AssertValue.checkNotNull("Name cannot be null", name);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Label{" +
        "name='" + name + '\'' +
        '}';
  }
}
