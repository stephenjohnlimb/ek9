package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests classes and inference with generics.
 */
class BadGenericClassesWithInferenceFullCompilationTest extends FullCompilationTest {

  public BadGenericClassesWithInferenceFullCompilationTest() {
    super("/examples/parseButFailCompile/badGenericClasses");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {

    assertFalse(program.getParsedModules("bad.classes.inference.example").isEmpty());
  }
}
