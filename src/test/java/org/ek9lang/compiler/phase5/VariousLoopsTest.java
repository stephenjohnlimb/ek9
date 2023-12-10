package org.ek9lang.compiler.phase5;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Designed to demonstrate correct loop statements and loop expressions.
 * With one deliberate failure to jsut check all the other examples compile correctly.
 */
class VariousLoopsTest extends PhasesTest {

  public VariousLoopsTest() {
    super("/examples/parseButFailCompile/loopStatementsExpressions",
        List.of("just.various.loops"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

  }
}
