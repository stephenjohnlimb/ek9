package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just test simple components all compile.
 */
class SimpleComponentsCompilationTest extends FullCompilationTest {

  public SimpleComponentsCompilationTest() {
    super("/examples/constructs/components");
  }

  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("net.customer", 4).test(program);
    new SymbolCountCheck("com.customer.components", 19).test(program);
  }
}
