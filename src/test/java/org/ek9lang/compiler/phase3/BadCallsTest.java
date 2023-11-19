package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad calls.
 */
class BadCallsTest extends PhasesTest {

  public BadCallsTest() {
    super("/examples/parseButFailCompile/badCalls",
        List.of("bad.functioncall.examples1",
            "bad.enumeratedtypecall.examples1",
            "bad.constrainedtypecall.examples1",
            "bad.recordcalls.examples1",
            "bad.classcalls.examples1",
            "bad.componentcalls.examples1",
            "bad.textcalls.examples1",
            "bad.abstractcalls.examples1",
            "bad.abstractuse.examples1"));
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
