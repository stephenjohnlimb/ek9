package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Abstract base for bad full resolution tests.
 */
abstract class BadFullResolutionTest extends PhasesTest {

  public BadFullResolutionTest(final String fromResourcesDirectory) {
    super(fromResourcesDirectory, List.of());
  }

  public BadFullResolutionTest(final String fromResourcesDirectory, final List<String> expectedModules) {
    super(fromResourcesDirectory, expectedModules);
  }

  public BadFullResolutionTest(final String fromResourcesDirectory,
                               final List<String> expectedModules,
                               final boolean verbose,
                               final boolean muteReportedErrors) {
    super(fromResourcesDirectory, expectedModules, verbose, muteReportedErrors, true);

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
