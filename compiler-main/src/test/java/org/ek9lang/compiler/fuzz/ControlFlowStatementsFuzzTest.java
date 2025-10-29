package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for control flow statement structure robustness.
 * Tests parser-level handling of malformed control flow keywords and structure.
 *
 * <p>Each test in fuzzCorpus/controlFlowStatements contains malformed statement structure:
 * - Missing required keywords (case, in, while)
 * - Missing indentation (INDENT tokens)
 * - Wrong keyword order (finally before catch)
 * - Orphan blocks (else without if, catch without try)
 * - Missing block bodies
 *
 * <p>We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class ControlFlowStatementsFuzzTest extends FuzzTestBase {

  public ControlFlowStatementsFuzzTest() {
    super("controlFlowStatements", CompilationPhase.PARSING);
  }

  @Test
  void testControlFlowStatementSyntaxRobustness() {
    final int fileCount = ek9Workspace.getSources().size();
    assertTrue(fileCount > 0, "Expecting files to be tested");
    System.out.println("ControlFlowStatementsFuzzTest: Found " + fileCount + " .ek9 files to test");
    runTests();
  }
}
