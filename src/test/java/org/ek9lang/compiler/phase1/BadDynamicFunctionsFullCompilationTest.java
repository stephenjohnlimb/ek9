package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
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
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.function.use").isEmpty());
  }
}
