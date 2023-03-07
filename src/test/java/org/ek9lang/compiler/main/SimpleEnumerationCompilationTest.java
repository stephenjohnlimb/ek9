package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.ek9lang.compiler.symbol.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test simple enumerations compile.
 */
class SimpleEnumerationCompilationTest extends FullCompilationTest {

  public SimpleEnumerationCompilationTest() {
    super("/examples/constructs/types");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    //There is an extra type created which is dynamic function.
    new SymbolCountCheck("com.customer.enumerations", 8).test(program);
    //Tests now in the ek9 source code via directives.
  }

}
