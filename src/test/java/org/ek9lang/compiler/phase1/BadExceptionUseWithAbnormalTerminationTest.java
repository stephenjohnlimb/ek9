package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad use of exceptions, which can only cause block abnormal termination.
 * This is a simple early static check.
 */
class BadExceptionUseWithAbnormalTerminationTest extends PhasesTest {


  public BadExceptionUseWithAbnormalTerminationTest() {
    super("/examples/parseButFailCompile/abnormalBlockTermination",
        List.of("bad.flowcontrol.examples"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
