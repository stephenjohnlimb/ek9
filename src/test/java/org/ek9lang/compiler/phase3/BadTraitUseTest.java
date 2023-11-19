package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad usage of traits.
 */
class BadTraitUseTest extends PhasesTest {

  public BadTraitUseTest() {
    super("/examples/parseButFailCompile/badTraitUse",
        List.of("missing.trait.implementations1",
            "mix.traits.implementation",
            "clashing.implementations",
            "additional.traits.by",
            "bad.trait.by.variables",
            "trait.with.trait.by"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

  }
}
