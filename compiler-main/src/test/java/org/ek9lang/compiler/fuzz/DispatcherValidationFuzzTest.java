package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for dispatcher validation errors in FULL_RESOLUTION phase.
 * Tests DISPATCHER_PURE_MISMATCH and DISPATCHER_PRIVATE_IN_SUPER constraints.
 *
 * <p>Test corpus: fuzzCorpus/dispatcherValidation (2 test files)
 * Validates dispatcher method purity and visibility requirements.
 *
 * <p>Test scenarios:
 * 1. dispatcher_pure_calling_nonpure.ek9 - Pure dispatcher with non-pure implementation
 * - Pattern: Dispatcher marked as pure with non-pure matching method
 * - Error: All dispatcher implementations must match purity of dispatcher entry point
 * - Tests DISPATCHER_PURE_MISMATCH (existing coverage: 1 test)
 * <br/>
 * 2. dispatcher_private_in_super.ek9 - Private method in superclass dispatcher chain
 * - Pattern: Base class private method with derived class dispatcher
 * - Error: Private methods in superclass cannot be used in dispatcher resolution
 * - Tests DISPATCHER_PRIVATE_IN_SUPER (existing coverage: 1 test)
 * </p>
 * <p>Dispatcher Constraint Semantics:
 * - DISPATCHER_PURE_MISMATCH: If dispatcher entry point is pure, all matching
 *   dispatcher methods must also be marked as pure
 * - DISPATCHER_PRIVATE_IN_SUPER: Private methods in superclasses are not visible
 *   to dispatcher resolution in derived classes
 * - Error detected at FULL_RESOLUTION phase during dispatcher validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect dispatcher purity violations at FULL_RESOLUTION phase
 * - Compiler should detect private method visibility issues in dispatcher chains
 * - Dispatcher entry point purity must match all dispatcher implementation methods
 * </p>
 * <p>Validates: Dispatcher validation ensures method resolution follows purity
 * and visibility rules, preventing runtime dispatch failures and maintaining
 * functional purity guarantees.
 * </p>
 * <p>Gap addressed: Critical dispatcher errors had minimal coverage:
 * - DISPATCHER_PURE_MISMATCH: 1 existing test → 2 total tests
 * - DISPATCHER_PRIVATE_IN_SUPER: 1 existing test → 2 total tests
 * Covers pure/non-pure mismatches and private method visibility in inheritance.
 * This ensures EK9's dispatcher system correctly enforces purity and access control.
 * </p>
 */
class DispatcherValidationFuzzTest extends FuzzTestBase {

  public DispatcherValidationFuzzTest() {
    super("dispatcherValidation", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testDispatcherValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
