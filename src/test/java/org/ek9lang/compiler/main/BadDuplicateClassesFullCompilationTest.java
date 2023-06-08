package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests duplicate classes with dynamic classes.
 */
class BadDuplicateClassesFullCompilationTest extends FullCompilationTest {

  public BadDuplicateClassesFullCompilationTest() {
    super("/examples/parseButFailCompile/badDuplicateClasses");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.duplicate.dynamicclassmethods").isEmpty());
  }
}