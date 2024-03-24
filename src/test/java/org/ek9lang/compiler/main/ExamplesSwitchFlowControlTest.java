package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesSwitchFlowControlTest extends PhasesTest {

  public ExamplesSwitchFlowControlTest() {
    super("/examples/switchFlowControl", false, false);
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.IR_ANALYSIS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    new SymbolCountCheck("com.customer.just.switches", 11).test(program);
  }
}
