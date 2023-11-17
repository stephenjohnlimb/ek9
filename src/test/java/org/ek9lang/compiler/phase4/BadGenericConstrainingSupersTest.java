package org.ek9lang.compiler.phase4;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests generic classes with constraints.
 */
class BadGenericConstrainingSupersTest extends PhasesTest {

  public BadGenericConstrainingSupersTest() {
    super("/examples/parseButFailCompile/badGenericSupersUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.POST_RESOLUTION_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.constraining.supers").isEmpty());
    assertFalse(program.getParsedModules("bad.generic.constraining.resolution1").isEmpty());
    assertFalse(program.getParsedModules("bad.generic.constraining.resolution2").isEmpty());
    assertFalse(program.getParsedModules("bad.functiondelegate.byrecord").isEmpty());
  }
}
