package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for application registration validation at FULL_RESOLUTION phase.
 * Tests 'register' statement validation with invalid types.
 *
 * <p>Test corpus: fuzzCorpus/applicationRegistration/phase6 (1 test file)
 * Validates:
 * <ul>
 *   <li>register_non_component_types.ek9 - Registering functions/classes/records (invalid)</li>
 * </ul>
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL with INCOMPATIBLE_GENUS errors
 *
 * <p>Validates: Application 'register' statement semantic rules.
 */
class ApplicationRegistrationPhase6FuzzTest extends FuzzTestBase {

  public ApplicationRegistrationPhase6FuzzTest() {
    super("applicationRegistration/phase6", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testRegisterStatementValidation() {
    assertTrue(runTests() != 0);
  }
}
