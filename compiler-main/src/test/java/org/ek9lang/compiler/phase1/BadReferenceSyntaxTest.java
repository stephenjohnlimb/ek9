package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests that references must include the '::' module qualifier.
 * INVALID_SYMBOL_BY_REFERENCE is checked in phase 3 (REFERENCE_CHECKS).
 */
class BadReferenceSyntaxTest extends PhasesTest {

  public BadReferenceSyntaxTest() {
    super("/examples/parseButFailCompile/phase1/badReferenceSyntax",
        List.of("bad.reference.syntax"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertFalse(compilationResult);
    // Expected: 3 INVALID_SYMBOL_BY_REFERENCE errors
    assertEquals(3, numberOfErrors);
  }
}
