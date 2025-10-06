package org.ek9lang.compiler.support;

import java.util.function.BiPredicate;

/**
 * Compares two multi-line strings line-by-lione for equality.
 * Useful for validating generated output (IR, bytecode) against expected output in directives.
 * Reports detailed differences including line numbers and content when strings don't match.
 */
public final class LineByLineComparator implements BiPredicate<String, String> {

  /**
   * Compare two multi-line strings line-by-line.
   * Lines are trimmed before comparison to handle whitespace variations.
   * Outputs detailed error information to System.err when differences are found.
   *
   * @param expected The expected string
   * @param actual   The actual string to compare
   * @return true if all lines match (after trimming), false otherwise
   */
  @Override
  public boolean test(final String expected, final String actual) {

    if (expected == null && actual == null) {
      System.err.println("Meaningless comparison");
      return false;
    }

    if (expected == null || actual == null) {
      System.err.println("Cannot be equal expected or actual is null");
      return false;
    }

    final String[] expectedLines = expected.split("\\r?\\n");
    final String[] actualLines = actual.split("\\r?\\n");

    if (expectedLines.length != actualLines.length) {
      System.err.printf("Number of lines differ: expected %d, actual %d%n",
          expectedLines.length, actualLines.length);
      // Continue comparing to show where they diverge
    }

    final int linesToCompare = Math.min(expectedLines.length, actualLines.length);
    for (int i = 0; i < linesToCompare; i++) {
      final var expectedLine = expectedLines[i].trim();
      final var actualLine = actualLines[i].trim();
      if (!expectedLine.equals(actualLine)) {
        System.err.printf("Line %d differs%n", i);
        System.err.println("Expected: [" + expectedLine + "]");
        System.err.println("Actual:   [" + actualLine + "]");
        return false;
      }
    }

    return expectedLines.length == actualLines.length;
  }
}
