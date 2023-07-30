package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad types used with generics.
 */
class BadTypesUsedWithGenericsFullCompilationTest extends FullCompilationTest {


  public BadTypesUsedWithGenericsFullCompilationTest() {
    super("/examples/parseButFailCompile/unresolvedTypeWithGenerics");
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.generics.use.types").isEmpty());
  }
}
