package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad function resolution, missing parameters etc.
 */
class BadFunctionResolutionTest extends PhasesTest {

  public BadFunctionResolutionTest() {
    super("/examples/parseButFailCompile/badFunctionResolution");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.functions.resolution").isEmpty());
    assertFalse(program.getParsedModules("auto.function.checks").isEmpty());
  }
}
