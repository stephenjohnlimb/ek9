package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesFlowControlSwitchTest extends SuccessfulTest {

  public ExamplesFlowControlSwitchTest() {
    super("/examples/flowControlSwitch");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    new SymbolCountCheck("com.customer.just.switches", 10).test(program);
  }
}
