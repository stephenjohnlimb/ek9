package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

class ExamplesLibConfigurationTest extends SuccessfulTest {

  public ExamplesLibConfigurationTest() {
    super("/examples/parseAndCompile/libConfiguration");
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    new SymbolCountCheck("simple.library.example", 7).test(program);

    new SymbolCountCheck("client.code.example", 14).test(program);

  }
}
