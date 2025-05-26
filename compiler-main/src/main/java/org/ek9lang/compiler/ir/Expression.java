package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.common.INodeVisitor;

/**
 * Models the idea of some form of expression (of which there is a myriad of forms).
 */
public final class Expression implements INode {

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

}
