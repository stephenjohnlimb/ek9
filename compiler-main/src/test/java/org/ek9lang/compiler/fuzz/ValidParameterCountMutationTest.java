package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Mutation tests for parameter count variations (Session 2).
 * Tests compiler robustness with varying parameter counts.
 *
 * <p>Test corpus: fuzzCorpus/mutations/valid/parameterCount (6 test files)
 * All tests should compile successfully - validates compiler stability with:
 * - Zero parameters
 * - Small counts (1, 5 parameters)
 * - Medium counts (10, 15 parameters)
 * - At the limit (20 parameters - MAX_ARGUMENTS_PER_CALL)
 *
 * <p>Expected behavior:
 * - All parameter count variations compile successfully (error count = 0)
 * - No stack overflow or memory issues
 * - Tests up to the compiler's MAX_ARGUMENTS_PER_CALL limit (20 params)
 *
 * <p>Validates: Compiler handles parameter counts from 0 to 20 (max limit) gracefully.
 * Tests symbol table, parser, and type resolution performance with varying parameter counts.
 * Note: Counts > 20 trigger E11010 "excessive complexity" error (tested separately in invalid mutations).
 */
class ValidParameterCountMutationTest extends FuzzTestBase {

  public ValidParameterCountMutationTest() {
    super("mutations/valid/parameterCount", CompilationPhase.PRE_IR_CHECKS, false);
  }

  @Test
  void testParameterCountVariations() {
    // runTests() returns FILE COUNT, not error count
    // For valid mutations: all 6 files should compile successfully
    assertEquals(6, runTests());
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                     final int numberOfErrors,
                                     final CompilableProgram program) {
    // For VALID mutations: expect SUCCESS, not errors
    assertTrue(compilationResult, "Valid parameter count mutations should compile successfully");
    assertEquals(0, numberOfErrors, "Valid parameter count mutations should have zero errors");
  }
}
