package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests PARAMETER_MISMATCH error when function is called with wrong parameter type.
 */
class BadParameterMismatchTest extends PhasesTest {

  public BadParameterMismatchTest() {
    super("/examples/parseButFailCompile/phase3/badParameterMismatch",
        List.of("bad.parameter.mismatch"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertFalse(compilationResult);
    // Expected error:
    // - 1 PARAMETER_MISMATCH (acceptsString called with Integer instead of String)
    assertEquals(1, numberOfErrors);
  }
}
