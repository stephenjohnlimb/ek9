package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesJustNamesTest extends SuccessfulTest {

  public ExamplesJustNamesTest() {
    super("/examples/parseAndCompile/justNames");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("just.names.check", 3).test(program);
  }
}
