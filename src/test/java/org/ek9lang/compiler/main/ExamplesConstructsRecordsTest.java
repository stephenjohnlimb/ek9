package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just test simple records all compile.
 */
class ExamplesConstructsRecordsTest extends PhasesTest {

  public ExamplesConstructsRecordsTest() {
    super("/examples/constructs/records");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
