package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad dynamic functions with dynamic variable capture usage.
 */
class BadDynamicFunctionsFullCompilationTest extends FullCompilationTest {

  public BadDynamicFunctionsFullCompilationTest() {
    super("/examples/parseButFailCompile/badDynamicFunctions");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(8, numberOfErrors);
    assertFalse(program.getParsedModules("bad.function.use").isEmpty());
  }
}
