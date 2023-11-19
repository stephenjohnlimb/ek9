package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad call resolution.
 * This covers a mix of calls to methods, function and function delegates.
 * Clearly this can get quite complex as all three can look the same!
 */
class BadCallResolutionTest extends PhasesTest {

  public BadCallResolutionTest() {
    super("/examples/parseButFailCompile/badCallResolution",
        List.of("bad.simple.resolution", "bad.detailed.resolution"));
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
