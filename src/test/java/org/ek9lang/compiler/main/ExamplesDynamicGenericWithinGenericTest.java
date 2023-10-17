package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

class ExamplesDynamicGenericWithinGenericTest extends PhasesTest {

  public ExamplesDynamicGenericWithinGenericTest() {
    super("/examples/dynamicGenericWithinGeneric");
  }

  @Test
  void testPhasedDevelopment() {
    //TODO fix up as generic as super is not quite working correctly.
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
