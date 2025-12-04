package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for extern module declaration validation.
 * Tests 'defines extern module' validation and body/abstract conflicts.
 *
 * <p>Test corpus: fuzzCorpus/externModuleValidation
 * Validates:
 * <ul>
 *   <li>extern_with_body.ek9 - Abstract method with body in extern (ABSTRACT_BUT_BODY_PROVIDED)</li>
 *   <li>non_extern_abstract_no_body.ek9 - Abstract method in non-abstract class</li>
 * </ul>
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL with appropriate errors
 *
 * <p>Validates: Extern module semantic rules for method body requirements.
 */
class ExternModuleValidationFuzzTest extends FuzzTestBase {

  public ExternModuleValidationFuzzTest() {
    super("externModuleValidation", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testExternModuleValidation() {
    assertTrue(runTests() != 0);
  }
}
