package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad use of exceptions, which can only cause block abnormal termination.
 * This is a simple early static check.
 */
class BadExceptionUseWithAbnormalTerminationTest extends FullCompilationTest {


  public BadExceptionUseWithAbnormalTerminationTest() {
    super("/examples/parseButFailCompile/abnormalBlockTermination");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(34, numberOfErrors);
    var alpha = program.getParsedModules("bad.flowcontrol.examples");
    assertNotNull(alpha);
  }
}
