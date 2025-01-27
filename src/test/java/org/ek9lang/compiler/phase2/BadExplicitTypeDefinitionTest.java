package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Abstract base for bad explicit type definitions..
 */
abstract class BadExplicitTypeDefinitionTest extends PhasesTest {


  public BadExplicitTypeDefinitionTest(final String fromResourcesDirectory) {
    super(fromResourcesDirectory, List.of());
  }

  public BadExplicitTypeDefinitionTest(final String fromResourcesDirectory, final List<String> expectedModules) {
    super(fromResourcesDirectory, expectedModules);
  }

  public BadExplicitTypeDefinitionTest(final String fromResourcesDirectory,
                                       final List<String> expectedModules,
                                       final boolean verbose,
                                       final boolean muteReportedErrors) {
    super(fromResourcesDirectory, expectedModules, verbose, muteReportedErrors, true);

  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
