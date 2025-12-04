package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for aspect declaration validation.
 * Tests 'with aspect of' clause validation with invalid types.
 *
 * <p>Test corpus: fuzzCorpus/aspectValidation
 * Validates:
 * <ul>
 *   <li>aspect_undefined_type.ek9 - Using undefined type as aspect (NOT_RESOLVED)</li>
 *   <li>aspect_string_literal.ek9 - Using string literal instead of call (PARSING error)</li>
 *   <li>aspect_missing_of_keyword.ek9 - 'with aspect' without 'of' (PARSING error)</li>
 * </ul>
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL with appropriate errors
 *
 * <p>Validates: Application aspect registration semantic rules.
 */
class AspectValidationFuzzTest extends FuzzTestBase {

  public AspectValidationFuzzTest() {
    super("aspectValidation", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testAspectValidation() {
    assertTrue(runTests() != 0);
  }
}
