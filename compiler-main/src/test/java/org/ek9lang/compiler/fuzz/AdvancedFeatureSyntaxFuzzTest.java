package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for advanced feature syntax robustness.
 * Tests parser-level handling of malformed ranges, dicts, streams, assert, generics, and object access.
 *
 * <p>Each test in fuzzCorpus/advancedFeatureSyntax contains malformed advanced syntax:
 * - Range expressions (missing ellipsis, incomplete start/end values)
 * - Dict literals (missing colons, incomplete key-value pairs)
 * - Stream expressions using 'cat' (missing pipe, trailing pipe)
 * - Stream expressions using 'for range' (missing pipe, missing collect, incomplete range)
 * - Assert statements (missing expression)
 * - Parameterized types (missing parentheses, missing OF keyword)
 * - Object access expressions (trailing dot)
 *
 * <p>We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class AdvancedFeatureSyntaxFuzzTest extends FuzzTestBase {

  public AdvancedFeatureSyntaxFuzzTest() {
    super("advancedFeatureSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testAdvancedFeatureSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
