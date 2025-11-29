package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for application registration validation at EXPLICIT_TYPE_SYMBOL_DEFINITION phase.
 * Tests 'with application of' clause validation with invalid types.
 *
 * <p>Test corpus: fuzzCorpus/applicationRegistration/phase4 (1 test file)
 * Validates:
 * <ul>
 *   <li>program_with_non_application.ek9 - Using trait/class/component/undefined as application (invalid)</li>
 * </ul>
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL with INCOMPATIBLE_GENUS or TYPE_NOT_RESOLVED errors
 *
 * <p>Validates: Program 'with application of' semantic rules.
 */
class ApplicationRegistrationPhase4FuzzTest extends FuzzTestBase {

  public ApplicationRegistrationPhase4FuzzTest() {
    super("applicationRegistration/phase4", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testProgramWithApplicationValidation() {
    assertTrue(runTests() != 0);
  }
}
