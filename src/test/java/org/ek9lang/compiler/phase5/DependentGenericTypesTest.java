package org.ek9lang.compiler.phase5;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Test dependent generic types, so not looking for error in this test (there are other test for that).
 */
class DependentGenericTypesTest extends PhasesTest {

  public DependentGenericTypesTest() {
    super("/examples/dependentGenericTypes",
        List.of("dependent.generic.types"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.PRE_IR_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);
  }
}
