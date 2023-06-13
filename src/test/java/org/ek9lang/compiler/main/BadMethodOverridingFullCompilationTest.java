package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad examples of overriding methods.
 */
class BadMethodOverridingFullCompilationTest extends FullCompilationTest {

  public BadMethodOverridingFullCompilationTest() {
    super("/examples/parseButFailCompile/badOverridingMethods");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.overriding.classmethods1").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.traitmethods1").isEmpty());
  }
}
