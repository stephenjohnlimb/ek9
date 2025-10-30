package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for ABSTRACT_BUT_BODY_PROVIDED semantic errors.
 * Tests SYMBOL_DEFINITION phase detection of abstract methods/functions with implementations.
 *
 * <p>Test corpus: fuzzCorpus/abstractBodyConflicts
 * Each test contains methods/functions marked 'abstract' but with body implementations.
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at SYMBOL_DEFINITION phase
 * - Error code: ABSTRACT_BUT_BODY_PROVIDED
 *
 * <p>Validates: AppropriateFunctionBodyOrError checker in phase 1.
 */
class AbstractBodyConflictsFuzzTest extends FuzzTestBase {

  public AbstractBodyConflictsFuzzTest() {
    super("abstractBodyConflicts", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testAbstractBodyConflictDetection() {
    final int fileCount = ek9Workspace.getSources().size();
    assertTrue(fileCount > 0, "Expecting files to be tested");
    System.out.println("AbstractBodyConflictsFuzzTest: Found " + fileCount + " .ek9 files to test");
    runTests();
  }
}
