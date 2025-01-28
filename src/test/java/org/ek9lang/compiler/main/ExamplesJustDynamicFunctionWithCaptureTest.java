package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesJustDynamicFunctionWithCaptureTest extends SuccessfulTest {

  public ExamplesJustDynamicFunctionWithCaptureTest() {
    super("/examples/justDynamicFunctionWithCapture");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("just.dynamicfunctions.check", 3).test(program);
  }
}
