package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad program usage.
 */
class BadProgramsCompilationTest extends FullCompilationTest {


  public BadProgramsCompilationTest() {
    super("/examples/parseButFailCompile/badPrograms");
  }


  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(9, numberOfErrors);

    var alpha = program.getParsedModules("bad.program.return");
    assertNotNull(alpha);

    var beta = program.getParsedModules("bad.argument.parameters");
    assertNotNull(beta);
  }
}
