package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesStreamBooksTest extends SuccessfulTest {

  public ExamplesStreamBooksTest() {
    super("/examples/streamBooks");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);
    new SymbolCountCheck("com.customer.books", 45).test(program);
  }
}
