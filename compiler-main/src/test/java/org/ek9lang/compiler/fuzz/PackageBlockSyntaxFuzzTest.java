package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for package block syntax validation in PARSING phase.
 * Tests malformed package block declarations.
 *
 * <p>Test corpus: fuzzCorpus/packageBlockSyntax
 * Covers syntax errors including:
 * - Empty package blocks (violates grammar + requirement)
 * - Wrong content type (constant vs variable)
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at PARSING phase
 * - Clear syntax error messages reported
 *
 * <p>Validates: Package block grammar enforcement.
 */
class PackageBlockSyntaxFuzzTest extends FuzzTestBase {

  public PackageBlockSyntaxFuzzTest() {
    super("packageBlockSyntax", CompilationPhase.PARSING, true);
  }

  @Test
  void testPackageBlockSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
