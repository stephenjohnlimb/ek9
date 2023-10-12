package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Test dependent generic types, so not looking for error in this test (there are other test for that).
 */
class DependentGenericTypesFullCompilationTest extends FullCompilationTest {

  public DependentGenericTypesFullCompilationTest() {
    super("/examples/dependentGenericTypes");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);
    assertFalse(program.getParsedModules("dependent.generic.types").isEmpty());
  }
}
