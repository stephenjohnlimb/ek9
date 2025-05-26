package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test constants compile.
 */
class ExamplesConstructsConstantsTest extends SuccessfulTest {

  public ExamplesConstructsConstantsTest() {
    super("/examples/constructs/constants");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    var moduleName = "net.customer";
    new SymbolCountCheck(2, moduleName, 26).test(program);
  }
}
