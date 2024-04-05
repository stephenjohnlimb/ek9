package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesStreamBooksTest extends PhasesTest {

  public ExamplesStreamBooksTest() {
    super("/examples/streamBooks", false, false);
  }

  @Test
  void testPhasedDevelopment() {
    //TODO move to full IR_ANALYSIS - once variable resolution is sorted out.
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.books", 45).test(program);
  }
}
