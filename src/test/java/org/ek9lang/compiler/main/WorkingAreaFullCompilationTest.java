package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests being worked on.
 * Sometimes it's good to have a small set of tests to focus on then when done put them in the appropriate space.
 */
class WorkingAreaFullCompilationTest extends FullCompilationTest {

  public WorkingAreaFullCompilationTest() {
    super("/examples/parseButFailCompile/workingarea");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    //Uncomment when ready to work on next set of tests.
    assertFalse(compilationResult);
  }
}
