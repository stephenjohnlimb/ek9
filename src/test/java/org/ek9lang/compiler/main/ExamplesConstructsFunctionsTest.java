package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test functions all compile.
 */
class ExamplesConstructsFunctionsTest extends SuccessfulTest {

  public ExamplesConstructsFunctionsTest() {
    super("/examples/constructs/functions");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("test.functions", 2).test(program);
    new SymbolCountCheck("com.customer.just.functions", 24).test(program);
    new SymbolCountCheck("com.customer.just", 6).test(program);
    new SymbolCountCheck("net.customer", 5).test(program);
  }

}
