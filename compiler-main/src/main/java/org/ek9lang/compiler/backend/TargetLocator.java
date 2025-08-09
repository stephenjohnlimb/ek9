package org.ek9lang.compiler.backend;

import java.util.function.Function;
import org.ek9lang.compiler.backend.jvm.JvmTarget;
import org.ek9lang.compiler.backend.llvm.cpp.LlvmCppTarget;
import org.ek9lang.core.CompilerException;
import org.ek9lang.core.TargetArchitecture;

/**
 * Given a TargetArchitecture, this function will provide the Target for it.
 * I don't expect there to be many Target Architectures.
 */
public class TargetLocator implements Function<TargetArchitecture, Target> {

  @Override
  public Target apply(final TargetArchitecture targetArchitecture) {

    return switch (targetArchitecture) {
      case LLVM_CPP -> new LlvmCppTarget();
      case JVM -> new JvmTarget();
      case NOT_SUPPORTED ->
          throw new CompilerException("Target architecture " + targetArchitecture + " is not supported");
    };
  }
}
