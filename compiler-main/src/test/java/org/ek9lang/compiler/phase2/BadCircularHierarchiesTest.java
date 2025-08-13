package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests circular hierarchies usage.
 */
class BadCircularHierarchiesTest extends PhasesTest {

  public BadCircularHierarchiesTest() {
    super("/examples/parseButFailCompile/phase2/circularHierarchies",
        List.of("bad.classes.hierarchies", "multiple.trait.use", "more.complex.trait.use"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
