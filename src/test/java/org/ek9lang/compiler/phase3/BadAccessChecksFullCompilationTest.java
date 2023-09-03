package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
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
    assertFalse(program.getParsedModules("bad.callthisandsuper.classmethod.access1").isEmpty());
    assertFalse(program.getParsedModules("bad.classfield.access").isEmpty());
    assertFalse(program.getParsedModules("bad.classmethod.access1").isEmpty());
    assertFalse(program.getParsedModules("bad.classmethod.access2").isEmpty());
    assertFalse(program.getParsedModules("bad.classmethod.access3").isEmpty());
    assertFalse(program.getParsedModules("bad.functiondelegates.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.higherfunctionandmethodcalls.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.recordfield.access").isEmpty());
  }
}
