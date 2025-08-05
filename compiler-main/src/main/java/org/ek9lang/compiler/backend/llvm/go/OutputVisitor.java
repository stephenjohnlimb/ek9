package org.ek9lang.compiler.backend.llvm.go;

import org.ek9lang.compiler.backend.ConstructTargetTuple;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * The visitor that produces llvm IR text, requires further processing for creation of '.o' files for a Construct.
 */
public final class OutputVisitor implements INodeVisitor {
  private final ConstructTargetTuple constructTargetTuple;

  public OutputVisitor(final ConstructTargetTuple constructTargetTuple) {
    AssertValue.checkNotNull("File cannot be null", constructTargetTuple.targetFile());
    this.constructTargetTuple = constructTargetTuple;
  }

  @Override
  public void visit() {
    visit(constructTargetTuple.construct());
  }

}
