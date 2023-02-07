package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just test methods all compile.
 */
class MethodsCompilationTest extends FullCompilationTest {

  public MethodsCompilationTest() {
    super("/examples/constructs/methods");
  }


  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.classandfield.resolution", 15).test(program);

    new SymbolCountCheck("com.customer.resolution", 12).test(program);

    new SymbolCountCheck("net.customer", 6).test(program);
  }
}
