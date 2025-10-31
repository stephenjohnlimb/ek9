package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for type block syntax validation in PARSING phase.
 * Tests malformed type block declarations.
 *
 * <p>Test corpus: fuzzCorpus/typeBlockSyntax
 * Covers syntax errors including:
 * - Empty type blocks (violates grammar + requirement)
 * - Missing 'as' keyword in type aliases
 *
 * <p>Existing Coverage: type_missing_identifier, enumeration_empty, constrain_missing_body
 * in declarationSyntax/ and advancedFeatureSyntax/
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at PARSING phase
 * - Clear syntax error messages reported
 *
 * <p>Validates: Type block grammar enforcement.
 */
class TypeBlockSyntaxFuzzTest extends FuzzTestBase {

  public TypeBlockSyntaxFuzzTest() {
    super("typeBlockSyntax", CompilationPhase.PARSING, true);
  }

  @Test
  void testTypeBlockSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
