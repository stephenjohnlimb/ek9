package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad usage of traits.
 */
class BadTraitUseTest extends PhasesTest {

  public BadTraitUseTest() {
    super("/examples/parseButFailCompile/badTraitUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("missing.trait.implementations1").isEmpty());
    assertFalse(program.getParsedModules("mix.traits.implementation").isEmpty());
    assertFalse(program.getParsedModules("clashing.implementations").isEmpty());
    assertFalse(program.getParsedModules("additional.traits.by").isEmpty());
    assertFalse(program.getParsedModules("bad.trait.by.variables").isEmpty());
    assertFalse(program.getParsedModules("trait.with.trait.by").isEmpty());
  }
}
