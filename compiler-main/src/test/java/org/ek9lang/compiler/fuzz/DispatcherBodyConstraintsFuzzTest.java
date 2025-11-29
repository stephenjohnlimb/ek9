package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for dispatcher body constraint validation.
 * Tests DISPATCHER_BUT_NO_BODY_PROVIDED constraint.
 *
 * <p>Test corpus: fuzzCorpus/dispatcherBodyConstraints (3 test files)
 * Validates that dispatcher methods must have a body implementation.
 *
 * <p>Test scenarios:
 * 1. dispatcher_no_body_multiple.ek9 - Multiple dispatchers without bodies
 * - Pattern: Two dispatcher methods (process, handle) without bodies
 * - Error: Each dispatcher needs its own body
 * - Tests DISPATCHER_BUT_NO_BODY_PROVIDED (2 errors)
 * <br/>
 * 2. dispatcher_no_body_with_return.ek9 - Dispatcher with return type but no body
 * - Pattern: Dispatcher declaring return type but no implementation
 * - Error: Return declaration doesn't substitute for body
 * - Tests DISPATCHER_BUT_NO_BODY_PROVIDED (1 error)
 * <br/>
 * 3. dispatcher_no_body_pure.ek9 - Pure dispatcher without body
 * - Pattern: Pure dispatcher method without implementation
 * - Error: Pure modifier doesn't change body requirement
 * - Tests DISPATCHER_BUT_NO_BODY_PROVIDED (1 error)
 * </p>
 * <p>Note: Basic dispatcher-with-parameter case covered in existing tests.
 * </p>
 * <p>Dispatcher Semantics:
 * - Dispatchers are polymorphic entry points that route to typed methods
 * - The dispatcher body handles unmatched types (fallback behavior)
 * - Without a body, there's no defined behavior for unmatched cases
 * - Error detected at SYMBOL_DEFINITION phase
 * </p>
 * <p>Expected behavior:
 * - Compiler should NOT crash on bodyless dispatchers (robustness)
 * - Compilation should FAIL with DISPATCHER_BUT_NO_BODY_PROVIDED
 * </p>
 * <p>Gap addressed: Dispatcher body constraint had minimal coverage:
 * - DISPATCHER_BUT_NO_BODY_PROVIDED: 1 existing test
 * - Now: 3 test files with 4 total error scenarios
 * Covers returns, multiple dispatchers, and pure modifier variations.
 * </p>
 */
class DispatcherBodyConstraintsFuzzTest extends FuzzTestBase {

  public DispatcherBodyConstraintsFuzzTest() {
    super("dispatcherBodyConstraints", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testDispatcherBodyConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}
