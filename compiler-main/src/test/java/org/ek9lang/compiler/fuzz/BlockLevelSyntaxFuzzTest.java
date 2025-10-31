package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for block-level syntax robustness.
 * Tests parser-level handling of malformed service, text, constant, program, and application blocks.
 *
 * <p>Each test in fuzzCorpus/blockLevelSyntax contains malformed block-level syntax:
 * - Service declarations (missing identifier, missing URI, invalid HTTP operations)
 * - Text block declarations (missing FOR keyword, missing string literal)
 * - Constant declarations (wrong arrow operator, missing identifier, missing value)
 * - Program and application blocks (tested but found to be valid without identifiers)
 *
 * <p>We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class BlockLevelSyntaxFuzzTest extends FuzzTestBase {

  public BlockLevelSyntaxFuzzTest() {
    super("blockLevelSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testBlockLevelSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
