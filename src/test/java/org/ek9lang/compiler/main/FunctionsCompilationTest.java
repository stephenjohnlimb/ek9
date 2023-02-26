package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test functions all compile.
 */
class FunctionsCompilationTest extends FullCompilationTest {

  public FunctionsCompilationTest() {
    super("/examples/constructs/functions");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("test.functions", 2).test(program);

    new SymbolCountCheck("com.customer.just.functions", 24).test(program);

    new SymbolCountCheck("com.customer.just", 6).test(program);

    new SymbolCountCheck("net.customer", 5).test(program);
  }

}
