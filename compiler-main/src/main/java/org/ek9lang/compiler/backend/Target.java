package org.ek9lang.compiler.backend;

import org.ek9lang.core.TargetArchitecture;

/**
 * The target platform/technology for the compiler output.
 */
public interface Target {

  /**
   * The Target Architecture for the output.
   *
   * @return One of the valid target architectures.
   */
  TargetArchitecture getArchitecture();

  /**
   * Is this target actually supported.
   * While the developer may have specified a valid TargetArchitecture, the
   * necessary components may not be available.
   * So for example, if the developer had specified LLVM_GO/LLVM_CPP as the target output
   * (and hence some form of binary as a result), it will be necessary for that
   * developer to have llvm installed (at the right level) and to be accessible.
   *
   * @return true if supported and output can be generated, false otherwise.
   */
  boolean isSupported();
}
