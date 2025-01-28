package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test references all compile and resolve.
 */
class ExamplesConstructsReferencesTest extends SuccessfulTest {

  public ExamplesConstructsReferencesTest() {
    super("/examples/constructs/references");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck(2, "net.customer.geometry", 5).test(program);
    new SymbolCountCheck(2, "net.customer.specials", 5).test(program);
    new SymbolCountCheck(1, "net.customer.pair.dev", 1).test(program);
    new SymbolCountCheck(1, "net.customer.pair", 8).test(program);
    new SymbolCountCheck(2, "net.customer.some", 3).test(program);
    new SymbolCountCheck(1, "ekopen.std.incs", 1).test(program);

  }
}
