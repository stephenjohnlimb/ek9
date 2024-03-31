package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

class ExamplesLibConfigurationTest extends PhasesTest {

  public ExamplesLibConfigurationTest() {
    super("/examples/libConfiguration", false, false);
  }

  @Test
  void testPhasedDevelopment() {
    testToPhase(CompilationPhase.IR_ANALYSIS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);

    new SymbolCountCheck("simple.library.example", 7).test(program);

    new SymbolCountCheck("client.code.example", 14).test(program);

  }
}
