package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.FullCompilationTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad usage of this and super.
 */
class BadThisAndSuperTest extends FullCompilationTest {

  public BadThisAndSuperTest() {
    super("/examples/parseButFailCompile/badThisAndSuper");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.functions.thisandsuper").isEmpty());
    assertFalse(program.getParsedModules("bad.classes.thisandsuper").isEmpty());
    assertFalse(program.getParsedModules("bad.components.thisandsuper").isEmpty());
  }
}
