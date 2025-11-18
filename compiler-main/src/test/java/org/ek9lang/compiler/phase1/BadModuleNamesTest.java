package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests that reserved module names (org.ek9.lang, org.ek9.math) cannot be used
 * outside of compiler bootstrapping.
 * <p>
 * Note: Cannot use @Error directives because INVALID_MODULE_NAME error occurs at
 * line 0 (module definition token issue), which prevents @Error directive matching.
 * This is documented in ERROR_RECOVERY_ISSUES.md
 * </p>
 */
class BadModuleNamesTest extends PhasesTest {

  public BadModuleNamesTest() {
    super("/examples/parseButFailCompile/phase1/badModuleNames",
        List.of("org.ek9.lang"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertFalse(compilationResult);
    // Expected: 1 INVALID_MODULE_NAME error
    assertEquals(1, numberOfErrors);
  }
}
