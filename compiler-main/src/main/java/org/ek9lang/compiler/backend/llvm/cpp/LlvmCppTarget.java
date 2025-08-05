package org.ek9lang.compiler.backend.llvm.cpp;

import org.ek9lang.compiler.backend.llvm.LlvmTarget;
import org.ek9lang.core.TargetArchitecture;

/**
 * Checks for C++ target architecture.
 */
public class LlvmCppTarget extends LlvmTarget {
  @Override
  public TargetArchitecture getArchitecture() {

    return TargetArchitecture.LLVM_CPP;
  }

  @Override
  public boolean isSupported() {
    //TODO also locate the c++ ek9 stdlibrary.
    return clangExecutableSupported;
  }

}
