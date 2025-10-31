package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for text interpolation syntax robustness.
 * Tests parser-level handling of malformed ${...} interpolation syntax.
 *
 * <p>Each subdirectory in fuzzCorpus/textInterpolationSyntax contains one invalid .ek9 file
 * with malformed interpolation syntax (unclosed braces, empty expressions, etc.).
 * We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class TextInterpolationSyntaxFuzzTest extends FuzzTestBase {

  public TextInterpolationSyntaxFuzzTest() {
    super("textInterpolationSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testTextInterpolationSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
