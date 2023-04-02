package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad generic parameters count on generics.
 */
class BadGenericParametersFullCompilationTest extends FullCompilationTest {

  public BadGenericParametersFullCompilationTest() {
    super("/examples/parseButFailCompile/badGenericDefinitions");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors,
                                    CompilableProgram program) {
    assertFalse(program.getParsedModules("incorrect.parameters.on.constructors").isEmpty());
  }
}
