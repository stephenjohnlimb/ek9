package org.ek9lang.compiler.phase5;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Designed to demonstrate correct loop statements and loop expressions.
 * With one deliberate failure to just check all the other examples compile correctly.
 */
class VariousComplexityCalculationsTest extends PhasesTest {

  public VariousComplexityCalculationsTest() {
    super("/examples/complexity",
        List.of("simple.ifcomplexity"), true, false);
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);

  }
}
