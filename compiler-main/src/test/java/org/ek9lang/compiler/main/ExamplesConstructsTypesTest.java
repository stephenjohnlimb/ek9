package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test simple types all compile.
 */
class ExamplesConstructsTypesTest extends SuccessfulTest {

  public ExamplesConstructsTypesTest() {
    super("/examples/parseAndCompile/constructs/types", false, true);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("com.customer.enumerations", 8).test(program);
  }
}
