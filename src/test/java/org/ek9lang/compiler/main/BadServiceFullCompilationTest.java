package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad references usage.
 */
class BadServiceFullCompilationTest extends FullCompilationTest {

  public BadServiceFullCompilationTest() {
    super("/examples/parseButFailCompile/badServiceDefinition");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(5, numberOfErrors);
    assertFalse(program.getParsedModules("bad.services.use").isEmpty());
  }
}
