package org.ek9lang.compiler.phase5;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Focus on use of the EK9 Result type.
 * Both valid and invalid use.
 */
class ResultAccessTest extends PhasesTest {

  public ResultAccessTest() {
    super("/examples/parseButFailCompile/phase5BadResultUse",
        List.of("error.on.result.access"));
  }

  @Test
  void testPhaseDevelopment() {
    //Move to PRE_IR_CHECKS when implementing Result method access.
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

  }
}
