package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesJustDynamicFunctionWithCaptureTest extends FullCompilationTest {

  public ExamplesJustDynamicFunctionWithCaptureTest() {
    super("/examples/justDynamicFunctionWithCapture");
  }

  @Test
  void testPhasedDevelopment() {
    //Issues with capture at the moment - for FULL_RESOLUTION
    //Needs some though on how to resolve and define with correct name when doing the capture
    //As dynamic functions and capture are taken out of the hierarchical scope stack.
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("just.dynamicfunctions.check", 3).test(program);
  }
}
