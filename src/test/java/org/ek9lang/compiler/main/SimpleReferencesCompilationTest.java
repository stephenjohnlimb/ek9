package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test references all compile and resolve.
 */
class SimpleReferencesCompilationTest extends FullCompilationTest {

  public SimpleReferencesCompilationTest() {
    super("/examples/constructs/references");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck(2, "net.customer.geometry", 5).test(program);
    new SymbolCountCheck(2, "net.customer.specials", 5).test(program);
    new SymbolCountCheck(1, "net.customer.pair.dev", 1).test(program);
    new SymbolCountCheck(1, "net.customer.pair", 8).test(program);
    new SymbolCountCheck(2, "net.customer.some", 3).test(program);
    new SymbolCountCheck(1, "ekopen.std.incs", 1).test(program);

  }
}
