package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests classes and inference with generics.
 */
class BadGenericClassesWithInferenceTest extends PhasesTest {

  public BadGenericClassesWithInferenceTest() {
    super("/examples/parseButFailCompile/badGenericClasses");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.classes.inference.example").isEmpty());
    assertFalse(program.getParsedModules("bad.genericnotparameterised.example").isEmpty());
  }
}
