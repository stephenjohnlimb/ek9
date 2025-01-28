package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Abstract base for simple successful tests.
 */
abstract class SuccessfulTest extends PhasesTest {

  private final CompilationPhase toPhase;

  public SuccessfulTest(final String fromResourcesDirectory) {
    this(fromResourcesDirectory, CompilationPhase.PRE_IR_CHECKS);
  }

  public SuccessfulTest(final String fromResourcesDirectory, final CompilationPhase toPhase) {
    super(fromResourcesDirectory);
    this.toPhase = toPhase;
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(toPhase);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
