package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad examples of overriding methods.
 */
class BadMethodOverridingTest extends PhasesTest {

  public BadMethodOverridingTest() {
    super("/examples/parseButFailCompile/badOverridingMethodsAndFunctions",
        List.of("bad.overriding.componentmethods1",
            "bad.overriding.classmethods1",
            "bad.overriding.classmethods2",
            "bad.overriding.classmethods3",
            "bad.overriding.classmethods4",
            "bad.overriding.classmethods5",
            "bad.overriding.traitmethods1",
            "bad.traits.covariance.examples",
            "bad.classes.covariance.examples",
            "bad.components.covariance.examples",
            "bad.functions.covariance.examples",
            "bad.overriding.functions"));
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
