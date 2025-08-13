package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test simple components all compile.
 */
class ExamplesConstructsComponentsTest extends SuccessfulTest {

  public ExamplesConstructsComponentsTest() {
    super("/examples/parseAndCompile/constructs/components", false, true);
  }


  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("net.customer", 4).test(program);
    new SymbolCountCheck("com.customer.components", 19).test(program);
  }
}
