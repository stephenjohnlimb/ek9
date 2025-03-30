package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * Models a simple assignment of a value to a variable.
 * In the EK9 this is pretty much just a reference being taken.
 * For implementations like llvm this will probably need to trigger a reference counting increment
 * to the actual bit of memory used.
 * Likewise with this variable (that has been assigned to) goes out of scope it will be necessary to
 * decrement the reference count.
 */
public final class Assignment implements INode {

  private final INode lhs;
  private final INode rhs;

  public Assignment(final INode lhs, final INode rhs) {
    AssertValue.checkNotNull("Left Assignment cannot be null", lhs);
    AssertValue.checkNotNull("Right Assignment cannot be null", rhs);
    this.lhs = lhs;
    this.rhs = rhs;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Assignment{" +
        "lhs=" + lhs +
        ", rhs=" + rhs +
        '}';
  }
}
