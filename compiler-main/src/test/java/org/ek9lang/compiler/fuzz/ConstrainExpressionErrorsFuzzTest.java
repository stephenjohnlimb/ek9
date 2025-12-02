package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for constraint expression resolution errors.
 *
 * <p>Tests METHOD_NOT_RESOLVED scenarios in constraint expressions:
 * - Using 'matches' operator on types that don't have it
 * - Using 'contains' operator on types that don't have it
 * - Type mismatches (e.g., Integer constraint with String literal)
 *
 * <p>These errors occur at FULL_RESOLUTION phase when the compiler
 * tries to resolve operators used in constraint expressions.
 */
class ConstrainExpressionErrorsFuzzTest extends FuzzTestBase {

  public ConstrainExpressionErrorsFuzzTest() {
    super("constrainExpressionErrors", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testConstrainExpressionErrorsRobustness() {
    assertTrue(runTests() != 0);
  }
}
