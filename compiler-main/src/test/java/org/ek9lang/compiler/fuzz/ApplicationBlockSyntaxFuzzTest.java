package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for application block syntax validation in PARSING phase.
 * Tests malformed application block declarations.
 *
 * <p>Test corpus: fuzzCorpus/applicationBlockSyntax
 * Covers syntax errors including:
 * - Missing application identifier
 * - Invalid statement types in application body
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at PARSING phase
 * - Clear syntax error messages reported
 *
 * <p>Validates: Application block grammar enforcement.
 */
class ApplicationBlockSyntaxFuzzTest extends FuzzTestBase {

  public ApplicationBlockSyntaxFuzzTest() {
    super("applicationBlockSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testApplicationBlockSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
