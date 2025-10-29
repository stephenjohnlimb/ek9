package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for control flow guard syntax robustness.
 * Tests parser-level handling of malformed guard syntax in if/switch/while/for/try statements.
 *
 * <p>Each test in fuzzCorpus/controlFlowGuards contains malformed guard syntax:
 * - Missing guard variables (if <- getValue())
 * - Wrong guard operators (if x <= getValue() instead of <-)
 * - Incomplete guard expressions (if x <-)
 * - Invalid operator combinations
 *
 * <p>We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class ControlFlowGuardsFuzzTest extends FuzzTestBase {

  public ControlFlowGuardsFuzzTest() {
    super("controlFlowGuards", CompilationPhase.PARSING);
  }

  @Test
  void testControlFlowGuardSyntaxRobustness() {
    final int fileCount = ek9Workspace.getSources().size();
    assertTrue(fileCount > 0, "Expecting files to be tested");
    System.out.println("ControlFlowGuardsFuzzTest: Found " + fileCount + " .ek9 files to test");
    runTests();
  }
}
