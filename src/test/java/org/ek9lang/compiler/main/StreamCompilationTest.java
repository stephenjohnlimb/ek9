package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test streams all compile.
 */
class StreamCompilationTest extends FullCompilationTest {

  public StreamCompilationTest() {
    super("/examples/streams");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.justcat", 6).test(program);

    new SymbolCountCheck("com.customer.justparagraphs", 3).test(program);

    new SymbolCountCheck("com.customer.justmoney", 1).test(program);

    new SymbolCountCheck("ekopen.io.file.examples", 1).test(program);

    new SymbolCountCheck("com.customer.books", 48).test(program);

    new SymbolCountCheck("com.customer.streams.collectas", 2).test(program);

    new SymbolCountCheck("com.customer.streams.splitter", 9).test(program);

  }
}
