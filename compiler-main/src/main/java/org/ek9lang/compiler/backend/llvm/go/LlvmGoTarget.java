package org.ek9lang.compiler.backend.llvm.go;

import org.ek9lang.compiler.backend.llvm.LlvmTarget;
import org.ek9lang.core.TargetArchitecture;

/**
 * Checks for golang target architecture.
 */
public class LlvmGoTarget extends LlvmTarget {
  @Override
  public TargetArchitecture getArchitecture() {

    return TargetArchitecture.LLVM_GO;
  }

  @Override
  public boolean isSupported() {
    //TODO also locate the go ek9 stdlibrary.
    return clangExecutableSupported;
  }

}
