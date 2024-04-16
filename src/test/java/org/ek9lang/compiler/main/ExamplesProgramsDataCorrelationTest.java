package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

class ExamplesProgramsDataCorrelationTest extends PhasesTest {

  public ExamplesProgramsDataCorrelationTest() {
    super("/examples/fullPrograms/dataCorrelation", false, false);
  }

  @Test
  void testPhasedDevelopment() {
    //TODO fixup generics and move to PRE_IR_CHECKS
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
