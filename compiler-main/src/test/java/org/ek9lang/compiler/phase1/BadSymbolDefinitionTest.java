package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Abstract base for bad symbol definition tests.
 */
abstract class BadSymbolDefinitionTest extends PhasesTest {

  public BadSymbolDefinitionTest(final String fromResourcesDirectory) {
    super(fromResourcesDirectory, List.of());
  }

  public BadSymbolDefinitionTest(final String fromResourcesDirectory, final List<String> expectedModules) {
    super(fromResourcesDirectory, expectedModules);
  }

  public BadSymbolDefinitionTest(final String fromResourcesDirectory,
                                       final List<String> expectedModules,
                                       final boolean verbose,
                                       final boolean muteReportedErrors) {
    super(fromResourcesDirectory, expectedModules, verbose, muteReportedErrors, true);

  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
