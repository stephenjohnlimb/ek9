package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesFlowControlIfTest extends SuccessfulTest {

  public ExamplesFlowControlIfTest() {
    super("/examples/parseAndCompile/flowControlIf");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    new SymbolCountCheck("com.customer.just.ifs", 6).test(program);
    new SymbolCountCheck("com.ifelse", 2).test(program);
    new SymbolCountCheck("com.ifguards", 4).test(program);

  }
}
