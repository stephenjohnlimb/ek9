package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test methods all compile.
 */
class ExamplesConstructsMethodsTest extends PhasesTest {

  public ExamplesConstructsMethodsTest() {
    super("/examples/constructs/methods");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.classandfield.resolution", 15).test(program);

    new SymbolCountCheck("com.customer.resolution", 12).test(program);

    new SymbolCountCheck("net.customer", 6).test(program);
  }
}
