package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Mutation tests for control flow nesting depth.
 * Tests compiler/parser robustness with deep nesting while staying within complexity limits.
 *
 * <p>Design Constraint: EK9 enforces cyclomatic complexity limits:
 * - Functions/Methods/Operators: max = 50
 * - Types/Classes: max = 500
 *
 * <p>Test Strategy: Stay within complexity limits while testing deep nesting:
 * - Single function tests: up to 48 levels (complexity 49, just under limit)
 * - Split function tests: 100-200 total levels split across multiple functions
 *
 * <p>Test corpus: fuzzCorpus/mutations/valid/nesting (9 test files)
 * All tests should compile successfully - validates compiler handles:
 * - Deep if statement nesting (10, 20, 40, 48 levels)
 * - Deep while loop nesting (20 levels)
 * - Deep for loop nesting (20 levels)
 * - Deep switch statement nesting (15 levels)
 * - Mixed control flow nesting (30 levels of if/while/for)
 * - Split nesting across functions (100, 200 total levels)
 *
 * <p>Expected behavior:
 * - All valid nesting tests compile successfully (error count = 0)
 * - Parser handles deep AST without stack overflow
 * - Complexity calculation correctly counts nested control structures
 * - IR generation handles deeply nested code
 *
 * <p>Validates: EK9 compiler properly handles deep control flow nesting
 * within designed complexity limits.
 *
 * @see InvalidNestingFuzzTest for tests that exceed complexity limits
 */
class ValidNestingMutationTest extends FuzzTestBase {

  private static final int EXPECTED_FILE_COUNT = 9;

  public ValidNestingMutationTest() {
    super("mutations/valid/nesting", CompilationPhase.PRE_IR_CHECKS, false);
  }

  @Test
  void testNestingRobustness() {
    assertEquals(EXPECTED_FILE_COUNT, runTests(),
        "Expected " + EXPECTED_FILE_COUNT + " valid nesting test files");
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                    final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult, "Valid nesting mutations should compile successfully");
    assertEquals(0, numberOfErrors, "Valid nesting mutations should have zero errors");
  }
}
