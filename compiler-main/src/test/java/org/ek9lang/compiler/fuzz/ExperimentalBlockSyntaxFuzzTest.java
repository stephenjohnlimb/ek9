package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * EXPERIMENTAL: Testing suspected block-level syntax errors.
 * This will reveal which are truly PARSING errors vs valid EK9 syntax.
 */
class ExperimentalBlockSyntaxFuzzTest extends FuzzTestBase {

  public ExperimentalBlockSyntaxFuzzTest() {
    super("experimentalBlockSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testExperimentalBlockSyntaxRobustness() {
    final int fileCount = ek9Workspace.getSources().size();
    assertTrue(fileCount > 0, "Expecting files to be tested");
    System.out.println("ExperimentalBlockSyntaxFuzzTest: Found " + fileCount + " .ek9 files to test");
    runTests();
  }
}
