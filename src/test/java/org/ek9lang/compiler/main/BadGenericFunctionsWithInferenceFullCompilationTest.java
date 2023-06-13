package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests functions and inference with generics.
 */
class BadGenericFunctionsWithInferenceFullCompilationTest extends FullCompilationTest {

  public BadGenericFunctionsWithInferenceFullCompilationTest() {
    super("/examples/parseButFailCompile/badGenericFunctions");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.functions.inference.example").isEmpty());
  }
}
