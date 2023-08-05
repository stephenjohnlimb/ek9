package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad program usage.
 */
class BadProgramsCompilationTest extends FullCompilationTest {


  public BadProgramsCompilationTest() {
    super("/examples/parseButFailCompile/badPrograms");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.program.return").isEmpty());
    assertFalse(program.getParsedModules("bad.argument.parameters").isEmpty());
  }
}