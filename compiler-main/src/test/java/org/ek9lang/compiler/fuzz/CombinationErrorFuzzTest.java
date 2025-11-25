package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for combination error scenarios in FULL_RESOLUTION phase.
 * Tests compiler's error recovery when multiple semantic errors occur in a single file.
 *
 * <p>Test corpus: fuzzCorpus/combinationErrors
 * Purpose: Validate that compiler reports independent errors without cascading false positives.
 *
 * <p>Test scenarios (in progress):
 * 1. type_and_method_resolution_failure.ek9 - TYPE_NOT_RESOLVED + METHOD_NOT_RESOLVED
 * - Tests compiler reports both type lookup failure and method resolution failure independently
 * - Validates error recovery doesn't cascade one error into multiple false positives
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect and report each semantic error independently
 * - Error recovery should allow subsequent error detection
 * - No cascading false positives from initial errors
 * </p>
 * <p>Validates: Multi-error scenarios and error recovery mechanisms work correctly.
 * Real-world code often contains multiple mistakes - this validates the compiler
 * handles complex error scenarios gracefully.
 * </p>
 */
class CombinationErrorFuzzTest extends FuzzTestBase {

  public CombinationErrorFuzzTest() {
    super("combinationErrors", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testCombinationErrorRobustness() {
    assertTrue(runTests() != 0);
  }
}
