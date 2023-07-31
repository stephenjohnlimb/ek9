package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad use of service methods.
 */
class BadServiceMethodsFullCompilationTest extends FullCompilationTest {

  public BadServiceMethodsFullCompilationTest() {
    super("/examples/parseButFailCompile/badServiceMethods");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);

    assertFalse(program.getParsedModules("bad.servicemethod.returntypes").isEmpty());
    assertFalse(program.getParsedModules("bad.servicemethod.argumenttypes").isEmpty());

  }
}
