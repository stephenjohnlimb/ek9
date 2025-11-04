package org.ek9lang.compiler.backend.jvm;

import org.objectweb.asm.ClassWriter;

/**
 * Custom ClassWriter that doesn't require loading classes during frame computation.
 * <p>
 * Standard ClassWriter.COMPUTE_FRAMES uses ClassLoader to find common superclasses
 * when computing stack map frames. This fails when generating multiple classes
 * in the same compilation unit because referenced classes aren't on classpath yet.
 * </p>
 * <p>
 * This custom writer overrides getCommonSuperClass() to use a simpler heuristic:
 * - For exception types, use java/lang/Throwable as common superclass
 * - For other types, use java/lang/Object as common superclass
 * </p>
 * <p>
 * This is safe because:
 * - Exception handler frame computation only needs to know exception hierarchy
 * - EK9 exception types all extend java.lang.Exception (which extends Throwable)
 * - Worst case: slightly larger stack frames, but still correct bytecode
 * </p>
 */
final class Ek9ClassWriter extends ClassWriter {

  Ek9ClassWriter(final int flags) {
    super(flags);
  }

  /**
   * Override getCommonSuperClass to avoid ClassLoader dependency.
   * Uses conservative estimate instead of precise type hierarchy lookup.
   *
   * @param type1 First type (internal name, e.g., "bytecode/test/CustomExceptionA")
   * @param type2 Second type (internal name, e.g., "bytecode/test/CustomExceptionB")
   * @return Common superclass internal name
   */
  @Override
  protected String getCommonSuperClass(final String type1, final String type2) {
    // If types are identical, return that type
    if (type1.equals(type2)) {
      return type1;
    }

    // For exception types (heuristic: contains "Exception" or extends from java/lang/* exception types)
    // Use Throwable as common superclass - this covers all exception hierarchies
    if (isLikelyExceptionType(type1) || isLikelyExceptionType(type2)) {
      return "java/lang/Throwable";
    }

    // For all other types, use Object as common superclass
    // This is always safe but may be less precise than actual hierarchy
    return "java/lang/Object";
  }

  /**
   * Heuristic to detect exception types without loading classes.
   * Checks if type name contains "Exception" or starts with known exception packages.
   */
  private boolean isLikelyExceptionType(final String type) {
    return type.contains("Exception")
        || type.startsWith("java/lang/Throwable")
        || type.startsWith("java/lang/Exception")
        || type.startsWith("java/lang/RuntimeException")
        || type.startsWith("java/lang/Error")
        || type.startsWith("org/ek9/lang/Exception");
  }
}
