package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Mutation tests for Unicode handling in strings and comments (Session 6).
 * Tests compiler robustness with international text and special Unicode characters.
 *
 * <p>Design Decision: EK9 uses ASCII-only identifiers by design (not a limitation).
 * All code identifiers must be in English (a-zA-Z) for universal readability.
 * Unicode IS fully supported in string literals and comments.
 *
 * <p>Test corpus: fuzzCorpus/mutations/valid/unicode (6 test files)
 * All tests should compile successfully - validates compiler handles:
 * - Basic Unicode scripts (Latin Extended, Cyrillic, CJK, Japanese, Korean)
 * - Emoji (basic, skin tones, gender variants, ZWJ sequences, flags)
 * - RTL (Right-to-Left) text (Arabic, Hebrew, BiDi mixing)
 * - Combining characters (diacriticals, zero-width, normalization forms)
 * - Unicode in comments (multilingual documentation)
 * - Mixed scenarios (real-world use cases, edge cases)
 *
 * <p>Expected behavior:
 * - All Unicode in strings compiles successfully (error count = 0)
 * - All Unicode in comments compiles successfully
 * - Parser correctly handles multi-byte UTF-8 sequences
 * - String operations work with Unicode content
 * - No encoding issues or character corruption
 *
 * <p>Validates: EK9 compiler properly handles Unicode in strings and comments
 * while maintaining ASCII-only identifiers by design.
 *
 * <p>Note: Invalid Unicode identifier tests are separate in parseButFailCompile/
 * as Unicode identifiers are rejected by design (English-only code policy).
 *
 * @see <a href="../../../../../../../EK9_UNICODE_DESIGN_DECISION.md">Unicode Design Decision</a>
 */
class ValidUnicodeMutationTest extends FuzzTestBase {

  public ValidUnicodeMutationTest() {
    super("mutations/valid/unicode", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testUnicodeInStringsAndComments() {
    // runTests() returns FILE COUNT, not error count
    // For valid mutations: all 6 files should compile successfully
    assertEquals(6, runTests());
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                     final int numberOfErrors,
                                     final CompilableProgram program) {
    // For VALID mutations: expect SUCCESS, not errors
    assertTrue(compilationResult, "Valid Unicode mutations should compile successfully");
    assertEquals(0, numberOfErrors, "Valid Unicode mutations should have zero errors");
  }
}
