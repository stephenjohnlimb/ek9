package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test composition compiles.
 */
class ExamplesConstructsCompositionTest extends SuccessfulTest {

  public ExamplesConstructsCompositionTest() {
    super("/examples/parseAndCompile/constructs/composition", false, true);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("com.customer.just.employees", 17).test(program);
  }
}
