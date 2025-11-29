package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Mutation tests for identifier length variations (Session 1).
 * Tests compiler robustness with varying identifier lengths.
 *
 * <p>Test corpus: fuzzCorpus/mutations/valid/identifierLength (3 initial test files)
 * All tests should compile successfully - validates compiler stability with:
 * - Very short identifiers (1 character)
 * - Long identifiers (50 characters)
 * - Very long identifiers (100 characters)
 *
 * <p>Expected behavior:
 * - All identifier length variations compile successfully (error count = 0)
 * - No crashes or hangs
 * - Compilation time reasonable for all identifier lengths
 *
 * <p>Validates: Compiler handles identifier lengths from 1 to 100+ characters gracefully.
 * Tests lexer, parser, and symbol table performance with varying identifier lengths.
 */
class ValidIdentifierLengthMutationTest extends FuzzTestBase {

  public ValidIdentifierLengthMutationTest() {
    super("mutations/valid/identifierLength", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testIdentifierLengthVariations() {
    // runTests() returns FILE COUNT, not error count
    // For valid mutations: all 3 files should compile successfully
    assertEquals(3, runTests());
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                     final int numberOfErrors,
                                     final CompilableProgram program) {
    // For VALID mutations: expect SUCCESS, not errors
    assertTrue(compilationResult, "Valid mutations should compile successfully");
    assertEquals(0, numberOfErrors, "Valid mutations should have zero errors");
  }
}
