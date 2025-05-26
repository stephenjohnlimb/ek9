package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test simple generics all compile.
 */
class ExamplesConstructsGenericsTest extends SuccessfulTest {

  public ExamplesConstructsGenericsTest() {
    super("/examples/constructs/generics");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("lists.plus.dictionaries", 6).test(program);
  }
}
