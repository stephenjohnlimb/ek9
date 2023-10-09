package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests for missing operators.
 */
class MIssingOperatorsFullCompilationTest extends FullCompilationTest {

  public MIssingOperatorsFullCompilationTest() {
    super("/examples/parseButFailCompile/missingOperators");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.defaulted.recordoperators").isEmpty());
    assertFalse(program.getParsedModules("bad.defaulted.classoperators").isEmpty());
  }
}
