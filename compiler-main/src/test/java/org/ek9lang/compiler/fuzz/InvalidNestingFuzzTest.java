package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for excessive nesting triggering EXCESSIVE_COMPLEXITY errors.
 * Tests that deep single-function nesting correctly triggers complexity limits.
 *
 * <p>Design Constraint: EK9 enforces cyclomatic complexity limits:
 * - Functions/Methods/Operators: max = 50
 * - Types/Classes: max = 500
 *
 * <p>Test corpus: fuzzCorpus/mutations/invalid/nesting (2 test files)
 * Validates that excessive nesting correctly triggers EXCESSIVE_COMPLEXITY:
 *
 * <p>Test scenarios:
 * 1. nesting_if_060.ek9 - 60 nested if statements
 *    - Complexity: 1 (base) + 60 (if blocks) = 61
 *    - Exceeds limit by 11
 *    - Expected: 1 EXCESSIVE_COMPLEXITY error
 * </p>
 * 2. nesting_if_100.ek9 - 100 nested if statements
 *    - Complexity: 1 (base) + 100 (if blocks) = 101
 *    - Exceeds limit by 51
 *    - Also stress tests parser with very deep AST
 *    - Expected: 1 EXCESSIVE_COMPLEXITY error
 *
 * <p>Expected behavior:
 * - Compiler detects functions exceeding 50 complexity threshold
 * - Deep nesting is counted correctly toward complexity
 * - Parser handles extreme nesting without crash (graceful error)
 *
 * <p>Validates: Cyclomatic complexity analysis correctly enforces limits
 * on deeply nested control flow structures.
 *
 * @see ValidNestingMutationTest for tests within complexity limits
 */
class InvalidNestingFuzzTest extends FuzzTestBase {

  public InvalidNestingFuzzTest() {
    super("mutations/invalid/nesting", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testExcessiveNestingComplexity() {
    assertTrue(runTests() != 0, "Expected invalid nesting tests to be processed");
  }
}
