package org.ek9lang.core;

/**
 * Possible supported target output architectures.
 */
public enum TargetArchitecture {
  JVM("jvm"),
  LLVM("llvm"),
  NOT_SUPPORTED("not-supported");

  private final String description;

  TargetArchitecture(final String description) {

    this.description = description;

  }

  public String getDescription() {

    return description;
  }
}
