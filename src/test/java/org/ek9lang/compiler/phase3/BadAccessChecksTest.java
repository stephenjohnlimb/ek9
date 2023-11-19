package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad access to fields and methods.
 */
class BadAccessChecksTest extends PhasesTest {

  public BadAccessChecksTest() {
    super("/examples/parseButFailCompile/badAccessChecks",
        List.of("bad.callthisandsuper.classmethod.access1",
            "bad.classfield.access",
            "bad.classmethod.access1",
            "bad.classmethod.access2",
            "bad.classmethod.access3",
            "bad.functiondelegates.examples",
            "bad.higherfunctionandmethodcalls.examples",
            "bad.recordfield.access",
            "bad.delegate.name.clashes"));
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
