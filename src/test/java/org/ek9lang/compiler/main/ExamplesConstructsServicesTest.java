package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test simple services all compile.
 */
class ExamplesConstructsServicesTest extends SuccessfulTest {

  public ExamplesConstructsServicesTest() {
    super("/examples/constructs/services");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    //We expect an additional aggregate because we create an aggregate for the base TextAggregate.

    new SymbolCountCheck("com.customer.services", 44).test(program);
    new SymbolCountCheck("com.customer.webserver", 2).test(program);
    new SymbolCountCheck("com.customer.html", 4).test(program);

  }
}
