package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests generics and detection of T and other conceptual types.
 */
class BadGenericTUseTest extends PhasesTest {

  public BadGenericTUseTest() {
    super("/examples/parseButFailCompile/badGenericTUse",
        List.of("bad.use.conceptual.parameters", "bad.generic.constraining.types"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
