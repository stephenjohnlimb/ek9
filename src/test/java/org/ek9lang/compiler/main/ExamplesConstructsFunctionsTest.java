package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test functions all compile.
 */
class ExamplesConstructsFunctionsTest extends PhasesTest {

  public ExamplesConstructsFunctionsTest() {
    super("/examples/constructs/functions");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
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
