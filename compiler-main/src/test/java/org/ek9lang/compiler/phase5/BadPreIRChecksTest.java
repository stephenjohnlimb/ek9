package org.ek9lang.compiler.phase5;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Abstract base for bad PRE_IR_CHECKS Tests.
 */
abstract class BadPreIRChecksTest extends PhasesTest {

  public BadPreIRChecksTest(final String fromResourcesDirectory) {
    this(fromResourcesDirectory, List.of());
  }

  public BadPreIRChecksTest(final String fromResourcesDirectory, final List<String> expectedModules) {
    this(fromResourcesDirectory, expectedModules, false, true);
  }

  public BadPreIRChecksTest(final String fromResourcesDirectory,
                               final List<String> expectedModules,
                               final boolean verbose,
                               final boolean muteReportedErrors) {
    super(fromResourcesDirectory, expectedModules, verbose, muteReportedErrors, true);

  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

  }
}
