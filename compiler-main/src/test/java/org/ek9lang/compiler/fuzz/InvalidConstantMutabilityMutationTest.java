package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * High-volume fuzzing tests for constant immutability enforcement.
 * Tests that ALL mutating operators fail with NOT_MUTABLE on ALL constant types.
 *
 * <p>This is a comprehensive fuzzing corpus testing EK9's immutability guarantees:
 * <ul>
 *   <li>17+ constant types (Integer, Float, String, Boolean, Date, Time, etc.)</li>
 *   <li>12 mutating operators (:=, ++, --, +=, -=, *=, /=, :=?, :=:, :~:, :^:, unset)</li>
 *   <li>Multiple value variations per type (boundary values, typical values)</li>
 * </ul>
 *
 * <p>Test corpus: fuzzCorpus/mutations/invalid/constantMutability
 * Target: ~400-600 @Error directives across 18+ test files
 *
 * <p>Expected behavior:
 * <ul>
 *   <li>All mutation attempts on constants should fail with NOT_MUTABLE</li>
 *   <li>Errors occur at FULL_RESOLUTION phase (Phase 6)</li>
 *   <li>Some operator/type combinations may fail with OPERATOR_NOT_DEFINED instead</li>
 *   <li>Both error types are valid - we're testing compiler responses to invalid mutations</li>
 * </ul>
 *
 * <p>Validates: EK9's fundamental immutability guarantees for constants defined
 * with {@code defines constant}.
 */
class InvalidConstantMutabilityMutationTest extends FuzzTestBase {

  public InvalidConstantMutabilityMutationTest() {
    super("mutations/invalid/constantMutability", CompilationPhase.FULL_RESOLUTION, false);
  }

  @Test
  void testConstantImmutabilityEnforced() {
    assertTrue(runTests() != 0, "Expected constant mutation tests to detect errors");
  }
}
