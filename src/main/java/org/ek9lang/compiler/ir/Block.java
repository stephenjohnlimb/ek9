package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.core.AssertValue;

/**
 * The concept of a block of code, i.e. zero or more instructions.
 */
public final class Block extends Instructions {
  private final IScope scope;
  private final Marker start;
  private final Marker end;

  public Block(final IScope scope, final Marker start, final Marker end) {
    AssertValue.checkNotNull("Scope cannot be null", scope);
    AssertValue.checkNotNull("Start cannot be null", start);
    AssertValue.checkNotNull("End cannot be null", end);

    this.scope = scope;
    this.start = start;
    this.end = end;
  }

  public Marker getStart() {
    return start;
  }

  public Marker getEnd() {
    return end;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @SuppressWarnings("checkstyle:OperatorWrap")
  @Override
  public String toString() {
    return "Block{" +
        "scope=" + scope +
        ", items=" + super.getItems() +
        ", start=" + start +
        ", end=" + end +
        '}';
  }
}
