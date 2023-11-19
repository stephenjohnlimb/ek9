package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Checks for viable/missing operators when defining constrained types.
 */
class BadOperatorsOnConstrainedTypesTest extends PhasesTest {

  public BadOperatorsOnConstrainedTypesTest() {
    super("/examples/parseButFailCompile/badConstrainedOperators",
        List.of("bad.constrainedtypeoperators.examples1"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
