package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesFlowControlIfTest extends PhasesTest {

  public ExamplesFlowControlIfTest() {
    super("/examples/flowControlIf", false, false);
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.IR_ANALYSIS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    new SymbolCountCheck("com.customer.just.ifs", 6).test(program);
    new SymbolCountCheck("com.ifelse", 2).test(program);
  }
}
