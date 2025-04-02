package org.ek9lang.compiler.backend;

import java.io.File;
import java.util.function.BiFunction;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.TargetArchitecture;

/**
 * Locates the appropriate IR node visitor for the appropriate target architecture.
 */
public final class OutputVisitorLocator implements BiFunction<TargetArchitecture, File, INodeVisitor> {

  @Override
  public INodeVisitor apply(final TargetArchitecture targetArchitecture, final File targetFile) {
    return switch (targetArchitecture) {
      case LLVM:
        yield new org.ek9lang.compiler.backend.llvm.OutputVisitor(targetFile);
      case JVM:
        yield new org.ek9lang.compiler.backend.jvm.OutputVisitor(targetFile);
      case NOT_SUPPORTED:
        throw new CompilerException(
            "Target architecture " + targetArchitecture + " is not supported");
    };
  }

}
