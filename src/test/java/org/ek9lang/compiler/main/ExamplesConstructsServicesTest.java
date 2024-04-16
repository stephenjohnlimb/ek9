package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test simple services all compile.
 */
class ExamplesConstructsServicesTest extends PhasesTest {

  public ExamplesConstructsServicesTest() {
    super("/examples/constructs/services", false, false);
  }


  @Test
  void testPhaseDevelopment() {
    //TODO fix up lots of errors and move to FULL_RESOLUTION
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    //We expect an additional aggregate because we create an aggregate for the base TextAggregate.

    new SymbolCountCheck("com.customer.services", 44).test(program);
    new SymbolCountCheck("com.customer.webserver", 2).test(program);
    new SymbolCountCheck("com.customer.html", 4).test(program);

  }
}
