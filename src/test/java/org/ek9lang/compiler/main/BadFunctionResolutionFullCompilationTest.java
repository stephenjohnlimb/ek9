package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad function resolution, missing parameters etc.
 */
class BadFunctionResolutionFullCompilationTest extends FullCompilationTest {

  public BadFunctionResolutionFullCompilationTest() {
    super("/examples/parseButFailCompile/badFunctionResolution");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(program.getParsedModules("bad.functions.resolution").isEmpty());
  }
}
