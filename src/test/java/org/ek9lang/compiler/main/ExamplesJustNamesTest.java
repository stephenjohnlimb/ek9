package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesJustNamesTest extends PhasesTest {

  public ExamplesJustNamesTest() {
    super("/examples/justNames");
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.IR_ANALYSIS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("just.names.check", 3).test(program);
  }
}
