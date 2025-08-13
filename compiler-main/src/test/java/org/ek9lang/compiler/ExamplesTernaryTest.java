package org.ek9lang.compiler;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.common.PhasesTest;
import org.ek9lang.compiler.support.SymbolCountCheck;
import org.junit.jupiter.api.Test;

/**
 * Test the ternary operations as used in the website all compile.
 */
class ExamplesTernaryTest extends PhasesTest {

  public ExamplesTernaryTest() {
    super("/examples/parseAndCompile/ternary");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult);
    final var moduleName = "com.customer.just.ternary";

    assertFalse(program.getParsedModules(moduleName).isEmpty());
    new SymbolCountCheck(moduleName, 1).test(program);
  }
}
