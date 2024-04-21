package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Just test composition compiles.
 */
class ExamplesConstructsCompositionTest extends PhasesTest {

  public ExamplesConstructsCompositionTest() {
    super("/examples/constructs/composition", false, false);
  }

  @Test
  void testPhaseDevelopment() {
    //TODO look at property initialisations to move or PRE_IR_CHECKS
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
    new SymbolCountCheck("com.customer.just.employees", 17).test(program);
  }
}
