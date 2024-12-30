package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

class ExamplesExtendingGenericScenariosTest extends PhasesTest {

  public ExamplesExtendingGenericScenariosTest() {
    super("/examples/extendingGenericScenarios");
  }

  @Test
  void testPhasedDevelopment() {
    //TODO fix up as generic as super is not quite working correctly.
    //This is because it does not look like the super type is being set when extending from a parameterised generic type
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
