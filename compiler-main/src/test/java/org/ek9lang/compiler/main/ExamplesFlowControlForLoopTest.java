package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesFlowControlForLoopTest extends SuccessfulTest {

  public ExamplesFlowControlForLoopTest() {
    super("/examples/parseAndCompile/flowControlForLoop");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("just.forloops.check", 5).test(program);

    new SymbolCountCheck("com.customer.loop", 20).test(program);

    new SymbolCountCheck("com.customer.preamble.loop", 4).test(program);

  }
}
