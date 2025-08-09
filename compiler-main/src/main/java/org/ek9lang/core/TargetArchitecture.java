package org.ek9lang.core;

/**
 * Possible supported target output architectures.
 */
public enum TargetArchitecture {
  JVM("jvm"),
  LLVM_CPP("llvm-cpp"),
  NOT_SUPPORTED("not-supported");

  private final String description;

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static TargetArchitecture from(final String description) {
    final var proposed = description.toUpperCase().replace("-", "_");
    try {
      return TargetArchitecture.valueOf(proposed);
    } catch (IllegalArgumentException _) {
      return TargetArchitecture.NOT_SUPPORTED;
    }
  }

  TargetArchitecture(final String description) {

    this.description = description;

  }

  public String getDescription() {

    return description;
  }
}
