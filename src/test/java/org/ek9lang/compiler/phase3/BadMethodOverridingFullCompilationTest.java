package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad examples of overriding methods.
 */
class BadMethodOverridingFullCompilationTest extends FullCompilationTest {

  public BadMethodOverridingFullCompilationTest() {
    super("/examples/parseButFailCompile/badOverridingMethodsAndFunctions");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.overriding.componentmethods1").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.componentmethods2").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.classmethods1").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.classmethods2").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.classmethods3").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.classmethods4").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.classmethods5").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.traitmethods1").isEmpty());
    assertFalse(program.getParsedModules("bad.traits.covariance.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.covariance.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.components.covariance.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.functions.covariance.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.overriding.functions").isEmpty());
  }
}
