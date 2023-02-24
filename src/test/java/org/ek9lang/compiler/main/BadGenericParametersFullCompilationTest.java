package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors,
                               CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(1, numberOfErrors);
    var alpha = program.getParsedModules("incorrect.parameters.on.constructors");
    assertNotNull(alpha);
  }
}
