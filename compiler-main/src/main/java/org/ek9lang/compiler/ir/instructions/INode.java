package org.ek9lang.compiler.ir.instructions;

import org.ek9lang.compiler.common.INodeVisitor;

/**
 * Models the concept of an Intermediate Representation construct.
 * This could be a simple concept of a whole aggregate.
 */
public interface INode {

  void accept(INodeVisitor visitor);
}
