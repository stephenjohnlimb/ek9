package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for module reference validation in REFERENCE_CHECKS phase.
 * Tests reference system errors that were completely untested.
 *
 * <p>Test corpus: fuzzCorpus/moduleReferences
 * Covers errors including:
 * - DUPLICATE_REFERENCE - Same module::symbol referenced twice
 * - REFERENCE_NOT_FOUND - Non-existent module or symbol
 * - AMBIGUOUS_REFERENCE - Multiple modules define same symbol name
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at REFERENCE_CHECKS phase
 * - Specific reference error messages reported
 *
 * <p>Validates: Module reference resolution system robustness.
 */
class ModuleReferenceFuzzTest extends FuzzTestBase {

  public ModuleReferenceFuzzTest() {
    super("moduleReferences", CompilationPhase.REFERENCE_CHECKS, false);
  }

  @Test
  void testModuleReferenceRobustness() {
    assertTrue(runTests() != 0);
  }
}
