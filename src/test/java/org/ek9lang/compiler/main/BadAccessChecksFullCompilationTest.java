package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad access to fields and methods.
 */
class BadAccessChecksFullCompilationTest extends FullCompilationTest {

  public BadAccessChecksFullCompilationTest() {
    super("/examples/parseButFailCompile/badAccessChecks");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.classfield.access").isEmpty());
    assertFalse(program.getParsedModules("bad.classmethod.access1").isEmpty());
    assertFalse(program.getParsedModules("bad.classmethod.access2").isEmpty());
    assertFalse(program.getParsedModules("bad.recordfield.access").isEmpty());
  }
}
