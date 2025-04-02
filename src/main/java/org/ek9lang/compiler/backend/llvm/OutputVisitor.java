package org.ek9lang.compiler.backend.llvm;

import java.io.File;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * The visitor that produces llvm IR text, requires further processing for creation of '.o' files for a Construct.
 */
public final class OutputVisitor implements INodeVisitor {
  private final File targetFile;

  public OutputVisitor(final File targetFile) {
    AssertValue.checkNotNull("File cannot be null", targetFile);
    this.targetFile = targetFile;
  }
}
