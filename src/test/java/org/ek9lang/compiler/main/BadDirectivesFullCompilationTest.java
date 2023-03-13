package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad directives. i.e. the very thing we use for testing - needs to be checked.
 */
class BadDirectivesFullCompilationTest extends FullCompilationTest {

  public BadDirectivesFullCompilationTest() {
    super("/examples/parseButFailCompile/badDirectives");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  protected boolean errorOnDirectiveErrors() {
    return false;
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

    assertFalse(program.getParsedModules("bad.error.directives").isEmpty());
    assertFalse(program.getParsedModules("bad.resolution.directives").isEmpty());
  }
}
