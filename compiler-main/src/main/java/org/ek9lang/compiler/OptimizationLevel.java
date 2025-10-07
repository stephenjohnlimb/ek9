package org.ek9lang.compiler;

/**
 * Defines the optimization levels available in the EK9 compiler.
 * Follows industry-standard GCC/Clang conventions for optimization flags.
 */
public enum OptimizationLevel {
  O0("o0"),
  O2("o2"),
  O3("o3");

  private final String description;

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static OptimizationLevel from(final String description) {
    final var proposed = description.toUpperCase().replace("-", "");
    try {
      return OptimizationLevel.valueOf(proposed);
    } catch (IllegalArgumentException _) {
      return OptimizationLevel.O2;
    }
  }

  OptimizationLevel(final String description) {

    this.description = description;

  }

  public String getDescription() {

    return description;
  }
}
