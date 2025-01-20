package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Test generic type parameterization and subsequent calls.
 * Shows different syntax to parameterize a generic type and also some that are incorrect.
 */
class GenericParameterizationAndCallsTest extends PhasesTest {

  public GenericParameterizationAndCallsTest() {
    super("/examples/parseButFailCompile/genericParameterizationAndCalls",
        List.of("generic.parameterization"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
