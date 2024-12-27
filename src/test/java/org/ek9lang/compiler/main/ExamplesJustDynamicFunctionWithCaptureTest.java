package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesJustDynamicFunctionWithCaptureTest extends PhasesTest {

  public ExamplesJustDynamicFunctionWithCaptureTest() {
    super("/examples/justDynamicFunctionWithCapture");
  }

  @Test
  void testPhasedDevelopment() {
    //As dynamic functions and capture are taken out of the hierarchical scope stack.
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("just.dynamicfunctions.check", 3).test(program);
  }
}
