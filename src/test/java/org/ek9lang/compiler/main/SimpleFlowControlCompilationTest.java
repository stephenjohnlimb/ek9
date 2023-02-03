package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just test simple flow control all compile.
 * So this is for loops, if/else, ternary, switches and yes exceptions!
 */
class SimpleFlowControlCompilationTest extends FullCompilationTest {

  public SimpleFlowControlCompilationTest() {
    super("/examples/flowControl");
  }

  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    //Now this should have some enumerations and records/functions.

    new SymbolCountCheck("com.customer.just.loops", 7).test(program);

    new SymbolCountCheck("com.customer.just.ifs", 5).test(program);

    new SymbolCountCheck("com.customer.just.switches", 3).test(program);

    new SymbolCountCheck("com.customer.just.ternary", 1).test(program);

    new SymbolCountCheck("com.customer.loop", 14).test(program);

    //Includes a dynamic class
    new SymbolCountCheck("com.customer.exceptions", 7).test(program);

    new SymbolCountCheck("com.ifelse", 1).test(program);
  }
}
