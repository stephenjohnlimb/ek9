package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad assignments usage.
 */
class BadAssignmentsTest extends PhasesTest {

  public BadAssignmentsTest() {
    super("/examples/parseButFailCompile/badAssignments");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.assignment.use").isEmpty());
    assertFalse(program.getParsedModules("bad.assignments.classes").isEmpty());
    assertFalse(program.getParsedModules("bad.coalescing.assignments").isEmpty());
  }
}
