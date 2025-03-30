package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * While on the surface just looks like a sequence of INodes (like Instructions).
 * This is semantically different in the sense that this represents a chained set of
 * interactions that are all linked.
 * i.e.
 * <pre>
 *   stdout.println("Hello, World")
 *   someRecord.prop1.withMethodCall("value").maybeAFunctionReturn()
 * </pre>
 * So when it comes to code generation this fact will be important.
 * Obviously the order of the nodes is important!
 */
public final class ChainedAccess implements INode {
  private final List<INode> items = new ArrayList<>();

  public void add(final INode item) {
    AssertValue.checkNotNull("Item cannot be null", item);
    items.add(item);
  }

  List<INode> getItems() {
    return List.copyOf(items);
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "ChainedAccess{" +
        "items=" + items +
        '}';
  }
}
