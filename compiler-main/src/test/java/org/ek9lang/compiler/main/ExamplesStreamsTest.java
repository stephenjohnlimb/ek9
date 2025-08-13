package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.support.SymbolCountCheck;

/**
 * Just test streams all compile.
 */
class ExamplesStreamsTest extends SuccessfulTest {

  public ExamplesStreamsTest() {
    super("/examples/parseAndCompile/streams");
  }


  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    super.assertFinalResults(compilationResult, numberOfErrors, program);

    new SymbolCountCheck("com.customer.justcat", 5).test(program);
    new SymbolCountCheck("com.customer.justparagraphs", 3).test(program);
    new SymbolCountCheck("com.customer.justmoney", 1).test(program);
    new SymbolCountCheck("ekopen.io.file.examples", 1).test(program);
    new SymbolCountCheck("com.customer.streams.collectas", 2).test(program);
    new SymbolCountCheck("com.customer.streams.splitter", 9).test(program);
  }
}
