package org.ek9lang.compiler.backend.jvm;

import java.io.File;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.Construct;
import org.ek9lang.core.AssertValue;

/**
 * The visitor that produces jvm bytecode for a Construct.
 */
public final class OutputVisitor implements INodeVisitor {
  private final File targetFile;

  public OutputVisitor(final File targetFile) {
    AssertValue.checkNotNull("File cannot be null", targetFile);
    this.targetFile = targetFile;
  }

  @Override
  public void visit(final Construct construct) {
    INodeVisitor.super.visit(construct);
  }
}
