package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad use of duplicated variables.
 */
class BadVariableDuplicationDeclarationTest extends FullCompilationTest {

  public BadVariableDuplicationDeclarationTest() {
    super("/examples/parseButFailCompile/existingVariables");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(program.getParsedModules("bad.variable.duplications").isEmpty());
  }
}
