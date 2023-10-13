package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Test generic type parameterization and subsequent calls.
 * Shows different syntax to parameterize a generic type and also some that are incorrect.
 */
class GenericParameterizationAndCallsFullCompilationTest extends FullCompilationTest {

  public GenericParameterizationAndCallsFullCompilationTest() {
    super("/examples/parseButFailCompile/genericParameterizationAndCalls");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("generic.parameterization").isEmpty());
  }
}
