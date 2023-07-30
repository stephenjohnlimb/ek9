package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad class modifier usage.
 */
class BadVariableOnlyDeclarationFullCompilationTest extends FullCompilationTest {


  public BadVariableOnlyDeclarationFullCompilationTest() {
    super("/examples/parseButFailCompile/badVariableOnlyDeclarations");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.variableonly.use").isEmpty());
  }
}
