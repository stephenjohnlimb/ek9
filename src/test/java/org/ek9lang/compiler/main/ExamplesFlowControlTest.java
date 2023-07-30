package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.symbols.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesFlowControlTest extends FullCompilationTest {

  public ExamplesFlowControlTest() {
    super("/examples/flowControl");
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.just.loops", 11).test(program);

    new SymbolCountCheck("com.customer.just.ifs", 6).test(program);

    new SymbolCountCheck("com.customer.just.switches", 6).test(program);

    new SymbolCountCheck("com.customer.just.ternary", 1).test(program);

    new SymbolCountCheck("com.customer.loop", 14).test(program);

    //Includes a dynamic class
    new SymbolCountCheck("com.customer.exceptions", 11).test(program);

    new SymbolCountCheck("com.ifelse", 1).test(program);
  }
}
