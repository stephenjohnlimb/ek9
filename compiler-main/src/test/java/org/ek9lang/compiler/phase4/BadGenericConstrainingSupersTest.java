package org.ek9lang.compiler.phase4;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests generic classes with constraints.
 */
class BadGenericConstrainingSupersTest extends PhasesTest {

  public BadGenericConstrainingSupersTest() {
    super("/examples/parseButFailCompile/phase4/badGenericSupersUse",
        List.of("bad.constraining.supers",
            "bad.generic.constraining.resolution1",
            "bad.generic.constraining.resolution2",
            "bad.generic.constraining.resolution3",
            "bad.functiondelegate.byrecord"), false, true);
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.POST_RESOLUTION_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
