package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Checks for attempts to mutate constants and enumerations.
 */
class BadConstantMutationTest extends PhasesTest {

  public BadConstantMutationTest() {
    super("/examples/parseButFailCompile/badConstantUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.mutations").isEmpty());
  }
}
