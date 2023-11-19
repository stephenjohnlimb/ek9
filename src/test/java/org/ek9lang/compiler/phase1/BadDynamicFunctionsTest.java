package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad dynamic functions with dynamic variable capture usage.
 */
class BadDynamicFunctionsTest extends PhasesTest {

  public BadDynamicFunctionsTest() {
    super("/examples/parseButFailCompile/badDynamicFunctions",
        List.of("bad.function.use"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
