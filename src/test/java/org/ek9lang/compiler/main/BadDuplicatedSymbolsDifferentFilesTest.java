package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.ek9lang.compiler.internals.CompilableProgram;
import org.ek9lang.compiler.main.phases.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Just tests duplicated symbols but in different files.
 */
class BadDuplicatedSymbolsDifferentFilesTest extends FullCompilationTest {

  public BadDuplicatedSymbolsDifferentFilesTest() {
    super("/examples/parseButFailCompile/duplicatedInDifferentFiles");
  }

  @Test
  void testReferencePhasedDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(3, numberOfErrors);
    var alpha = program.getParsedModules("duplications");
    assertNotNull(alpha);
  }
}
