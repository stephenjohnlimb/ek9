package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad calls.
 */
class BadCallsTest extends PhasesTest {

  public BadCallsTest() {
    super("/examples/parseButFailCompile/badCalls");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.functioncall.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.enumeratedtypecall.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.constrainedtypecall.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.recordcalls.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.classcalls.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.componentcalls.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.textcalls.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.abstractcalls.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.abstractuse.examples1").isEmpty());
  }
}
