package org.ek9lang.compiler.backend;

import java.util.function.Function;
import org.ek9lang.core.CompilerException;

/**
 * Locates the appropriate main entry visitor for the appropriate target architecture.
 * Follows the same pattern as OutputVisitorLocator for consistent multi-target support.
 */
public final class MainEntryVisitorLocator implements Function<MainEntryTargetTuple, IMainEntryVisitor> {

  @Override
  public IMainEntryVisitor apply(final MainEntryTargetTuple mainEntryTargetTuple) {
    return switch (mainEntryTargetTuple.compilerFlags().getTargetArchitecture()) {
      case LLVM_CPP -> new org.ek9lang.compiler.backend.llvm.cpp.MainEntryVisitor(mainEntryTargetTuple);
      case JVM -> new org.ek9lang.compiler.backend.jvm.MainEntryVisitor(mainEntryTargetTuple);
      case NOT_SUPPORTED -> throw new CompilerException(
          "Target architecture " + mainEntryTargetTuple.compilerFlags().getTargetArchitecture()
              + " is not supported for main entry generation");
    };
  }
}