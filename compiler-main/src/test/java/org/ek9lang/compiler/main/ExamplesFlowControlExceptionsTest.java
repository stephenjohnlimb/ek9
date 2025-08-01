package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesFlowControlExceptionsTest extends SuccessfulTest {

  public ExamplesFlowControlExceptionsTest() {
    super("/examples/flowControlExceptions", false, true);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    //Includes a dynamic class
    new SymbolCountCheck("com.customer.exceptions", 11).test(program);

  }
}
