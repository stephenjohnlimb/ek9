package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad method usage on a range of constructs.
 */
class BadMethodUseFullCompilationTest extends FullCompilationTest {

  public BadMethodUseFullCompilationTest() {
    super("/examples/parseButFailCompile/badMethodUse");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.traits.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.examples").isEmpty());
    assertFalse(program.getParsedModules("bad.components.examples").isEmpty());
  }
}
