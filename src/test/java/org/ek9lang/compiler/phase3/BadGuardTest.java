package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad usage of guard use.
 */
class BadGuardTest extends PhasesTest {

  public BadGuardTest() {
    super("/examples/parseButFailCompile/badGuards",
        List.of("some.bad.ifguards",
            "some.bad.whileguards",
            "some.bad.dowhileguards",
            "some.bad.forloopguards",
            "some.bad.forrangeguards")
      );
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
