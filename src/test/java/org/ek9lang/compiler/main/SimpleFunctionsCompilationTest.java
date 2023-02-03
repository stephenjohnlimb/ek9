package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just test simple functions all compile.
 */
class SimpleFunctionsCompilationTest extends FullCompilationTest {

  public SimpleFunctionsCompilationTest() {
    super("/examples/constructs/functions");
  }


  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("test.functions", 2).test(program);

    new SymbolCountCheck("com.customer.just.functions", 24).test(program);

    new SymbolCountCheck("com.customer.just", 6).test(program);

    new SymbolCountCheck("net.customer", 5).test(program);
  }

}
