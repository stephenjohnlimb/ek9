package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for dispatcher method resolution edge cases in FULL_RESOLUTION phase.
 * Tests complex dispatcher inheritance scenarios with minimal existing coverage.
 *
 * <p>Test corpus: fuzzCorpus/dispatchers (7 test files)
 * Covers dispatcher-specific error types:
 * - DISPATCHER_PRIVATE_IN_SUPER - Private dispatcher in superclass, public in derived
 * - DISPATCHER_PURE_MISMATCH - Pure dispatcher in superclass, non-pure in derived
 * - COVARIANCE_MISMATCH - Invalid covariant return types in dispatcher override
 * - INCOMPATIBLE_TYPES - Invalid contravariant parameters in dispatcher override
 *
 * <p>Test scenarios:
 * 1. dispatcher_private_in_super.ek9 - Access modifier violation (DISPATCHER_PRIVATE_IN_SUPER)
 * 2. dispatcher_purity_mismatch.ek9 - Purity requirement violation (DISPATCHER_PURE_MISMATCH)
 * 3. dispatcher_covariant_return_invalid.ek9 - Invalid return type covariance (COVARIANCE_MISMATCH)
 * 4. dispatcher_parameter_contravariance_invalid.ek9 - Invalid parameter contravariance (INCOMPATIBLE_TYPES)
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at FULL_RESOLUTION phase
 * - Specific dispatcher resolution error messages reported
 *
 * <p>Validates: Dispatcher method resolution system handles inheritance edge cases correctly.
 * Gap addressed: Only 1-2 existing tests per dispatcher error type.
 */
class DispatcherResolutionFuzzTest extends FuzzTestBase {

  public DispatcherResolutionFuzzTest() {
    super("dispatchers", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testDispatcherResolutionRobustness() {
    assertTrue(runTests() != 0);
  }
}
