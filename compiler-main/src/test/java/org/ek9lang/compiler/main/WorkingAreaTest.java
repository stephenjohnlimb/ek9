package org.ek9lang.compiler.main;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests being worked on.
 * Sometimes it's good to have a small set of tests to focus on then when done put them in the appropriate space.
 */
class WorkingAreaTest extends PhasesTest {

  public WorkingAreaTest() {
    super("/examples/parseButFailCompile/workingarea", true, false);
  }

  /**
   * For this test I've like to enable debugging output in the IR and Code generation (once implemented).
   *
   * @return true enable debug instrumentation.
   */
  @Override
  protected boolean addDebugInstrumentation() {
    return true;
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    // Testing CANNOT_EXTEND_IMPLEMENT_ITSELF - expect failure with error
    System.out.println("Compilation result: " + compilationResult);
    System.out.println("Number of errors: " + numberOfErrors);
  }
}
