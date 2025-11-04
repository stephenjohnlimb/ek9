package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for module reference validation in REFERENCE_CHECKS phase.
 * Comprehensive test suite covering 7 unique module reference error scenarios.
 *
 * <p>Test corpus: fuzzCorpus/moduleReferences (7 test files)
 * Covers all 4 reference error types:
 * - REFERENCES_CONFLICT - Same module::symbol referenced multiple times
 * - CONSTRUCT_REFERENCE_CONFLICT - Module definition conflicts with its own reference
 * - REFERENCE_DOES_NOT_RESOLVED - Non-existent module or symbol
 * - INVALID_SYMBOL_BY_REFERENCE - Unqualified reference (missing :: separator)
 *
 * <p>Test scenarios:
 * 1. duplicate_reference_exact.ek9 - Duplicate reference detection (REFERENCES_CONFLICT)
 * 2. self_reference.ek9 - Circular self-reference (CONSTRUCT_REFERENCE_CONFLICT)
 * 3. missing_module.ek9 - Non-existent module (REFERENCE_DOES_NOT_RESOLVED)
 * 4. reference_nonexistent_symbol.ek9 - Non-existent symbol in valid module
 * 5. duplicate_different_case.ek9 - Case sensitivity testing (2 variants)
 * 6. reference_to_builtin_base.ek9 - Built-in type reference attempt
 * 7. unqualified_symbol_conflict.ek9 - Missing :: qualifier (INVALID_SYMBOL_BY_REFERENCE)
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at REFERENCE_CHECKS phase
 * - Specific reference error messages reported
 *
 * <p>Validates: Module reference resolution system robustness without redundant test variations.
 */
class ModuleReferenceFuzzTest extends FuzzTestBase {

  public ModuleReferenceFuzzTest() {
    super("moduleReferences", CompilationPhase.REFERENCE_CHECKS);
  }

  @Test
  void testModuleReferenceRobustness() {
    assertTrue(runTests() != 0);
  }
}
