package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.core.AssertValue;

/**
 * The concept of a block of code, i.e. zero or more statements/expressions.
 */
public class Block implements INode {
  private final IScope scope;
  private final List<INode> items = new ArrayList<>();
  private final Label start;
  private final Label end;

  public Block(final IScope scope, final Label start, final Label end) {
    AssertValue.checkNotNull("Scope cannot be null", scope);
    AssertValue.checkNotNull("Start cannot be null", start);
    AssertValue.checkNotNull("End cannot be null", end);

    this.scope = scope;
    this.start = start;
    this.end = end;
  }

  public void add(final INode item) {
    AssertValue.checkNotNull("Item cannot be null", item);
    items.add(item);
  }

  public Label getStart() {
    return start;
  }

  public Label getEnd() {
    return end;
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Block{" +
        "scope=" + scope +
        ", items=" + items +
        ", start=" + start +
        ", end=" + end +
        '}';
  }
}
