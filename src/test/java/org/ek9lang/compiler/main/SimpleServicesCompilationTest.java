package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test simple services all compile.
 */
class SimpleServicesCompilationTest extends FullCompilationTest {

  public SimpleServicesCompilationTest() {
    super("/examples/constructs/services");
  }


  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    new SymbolCountCheck("com.customer.services", 43).test(program);

    new SymbolCountCheck("com.customer.webserver", 2).test(program);

    new SymbolCountCheck("com.customer.html", 3).test(program);

  }
}
