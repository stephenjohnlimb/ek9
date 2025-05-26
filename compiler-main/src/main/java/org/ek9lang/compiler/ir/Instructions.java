package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * The concept of an order list of instructions.
 */
public class Instructions implements INode {
  private final List<INode> items = new ArrayList<>();

  public void add(final INode item) {
    AssertValue.checkNotNull("Item cannot be null", item);
    items.add(item);
  }

  public List<INode> getItems() {
    return List.copyOf(items);
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Instructions{" +
        "items=" + items +
        '}';
  }
}
