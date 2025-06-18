package org.ek9lang.compiler.backend;

import java.util.function.Function;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.CompilerException;

/**
 * Locates the appropriate IR node visitor for the appropriate target architecture.
 */
public final class OutputVisitorLocator implements Function<ConstructTargetTuple, INodeVisitor> {

  @Override
  public INodeVisitor apply(final ConstructTargetTuple constructTargetTuple) {
    return switch (constructTargetTuple.compilerFlags().getTargetArchitecture()) {
      case LLVM -> new org.ek9lang.compiler.backend.llvm.OutputVisitor(constructTargetTuple);
      case JVM -> new org.ek9lang.compiler.backend.jvm.OutputVisitor(constructTargetTuple);
      case NOT_SUPPORTED -> throw new CompilerException(
          "Target architecture " + constructTargetTuple.compilerFlags().getTargetArchitecture()
              + " is not supported");
    };
  }

}
