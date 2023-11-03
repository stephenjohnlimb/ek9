package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just test dynamic examples compile.
 */
class ExamplesDynamicTest extends PhasesTest {

  public ExamplesDynamicTest() {
    super("/examples/dynamic");
  }

  @Test
  void testPhaseDevelopment() {
    //TODO implement the trait 'by' so that methods that are implemented in the 'by' are made synthetic in the class.
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
