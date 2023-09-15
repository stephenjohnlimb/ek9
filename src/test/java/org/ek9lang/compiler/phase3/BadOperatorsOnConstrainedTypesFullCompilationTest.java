package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Checks for viable/missing operators when defining constrained types.
 */
class BadOperatorsOnConstrainedTypesFullCompilationTest extends FullCompilationTest {

  public BadOperatorsOnConstrainedTypesFullCompilationTest() {
    super("/examples/parseButFailCompile/badConstrainedOperators");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.constrainedtypeoperators.examples1").isEmpty());
  }
}
