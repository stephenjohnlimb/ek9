package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesFlowControlWhileLoopTest extends SuccessfulTest {

  public ExamplesFlowControlWhileLoopTest() {
    super("/examples/parseAndCompile/flowControlWhileLoop");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("com.customer.just.loops", 4).test(program);
  }
}
