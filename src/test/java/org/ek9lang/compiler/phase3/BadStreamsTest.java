package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad stream combinations. There are lots!
 */
class BadStreamsTest extends PhasesTest {

  public BadStreamsTest() {
    super("/examples/parseButFailCompile/badStreams",
        List.of("bad.streams1", "bad.streams2", "bad.streams3", "bad.streams4",
            "bad.streams5", "bad.streams6", "bad.streams7", "bad.streams8"));
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
