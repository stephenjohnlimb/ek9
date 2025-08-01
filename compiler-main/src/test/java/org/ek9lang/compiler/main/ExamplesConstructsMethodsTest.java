package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test methods all compile.
 */
class ExamplesConstructsMethodsTest extends SuccessfulTest {

  public ExamplesConstructsMethodsTest() {
    super("/examples/constructs/methods", false, true);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("com.classandfield.resolution", 15).test(program);

    new SymbolCountCheck("com.customer.resolution", 12).test(program);

    new SymbolCountCheck("net.customer", 6).test(program);
  }
}
