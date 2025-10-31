package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for program block syntax validation in PARSING phase.
 * Tests malformed program block declarations.
 *
 * <p>Test corpus: fuzzCorpus/programBlockSyntax
 * Covers syntax errors including:
 * - Empty program blocks (violates grammar + requirement)
 * - Missing INDENT tokens
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at PARSING phase
 * - Clear syntax error messages reported
 *
 * <p>Validates: Program block grammar enforcement.
 */
class ProgramBlockSyntaxFuzzTest extends FuzzTestBase {

  public ProgramBlockSyntaxFuzzTest() {
    super("programBlockSyntax", CompilationPhase.PARSING, true);
  }

  @Test
  void testProgramBlockSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
