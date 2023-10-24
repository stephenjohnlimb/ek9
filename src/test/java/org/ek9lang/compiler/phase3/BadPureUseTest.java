package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad pure usage.
 */
class BadPureUseTest extends PhasesTest {

  public BadPureUseTest() {
    super("/examples/parseButFailCompile/badPureUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.pure.scenarios1").isEmpty());
    assertFalse(program.getParsedModules("bad.pure.scenarios2").isEmpty());
    assertFalse(program.getParsedModules("bad.pure.expressions").isEmpty());
  }
}
