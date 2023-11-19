package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests functions and inference with generics.
 */
class BadGenericFunctionsWithInferenceTest extends PhasesTest {

  public BadGenericFunctionsWithInferenceTest() {
    super("/examples/parseButFailCompile/badGenericFunctions",
        List.of("bad.functions.inference.example"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
