package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad constrained type use.
 */
class BadContainedTypeTest extends PhasesTest {

  public BadContainedTypeTest() {
    super("/examples/parseButFailCompile/badConstrainedTypes");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.constrainedtype.examples1").isEmpty());
    assertFalse(program.getParsedModules("bad.constrainedtype.examples2").isEmpty());
  }
}
