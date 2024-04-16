package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesFlowControlForLoopTest extends PhasesTest {

  public ExamplesFlowControlForLoopTest() {
    super("/examples/flowControlForLoop", false, false);
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("just.forloops.check", 5).test(program);

    new SymbolCountCheck("com.customer.loop", 20).test(program);

    new SymbolCountCheck("com.customer.preamble.loop", 4).test(program);

  }
}
