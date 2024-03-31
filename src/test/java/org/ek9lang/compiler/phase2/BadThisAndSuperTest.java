package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad usage of this and super.
 */
class BadThisAndSuperTest extends PhasesTest {

  public BadThisAndSuperTest() {
    super("/examples/parseButFailCompile/badThisAndSuper",
        List.of("bad.functions.thisandsuper", "bad.classes.thisandsuper", "bad.components.thisandsuper"),
        false, true);
  }

  @Test
  void testPhaseDevelopment() {
    //Need to fix up code resolution and move to FULL_RESOLUTION
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
