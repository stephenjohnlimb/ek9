package org.ek9lang.compiler.phase1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests bad references usage.
 * As this uses multiple files and multithreaded compiler it is not possible to use the '@directive' approach
 * for errors.
 */
class BadReferencesTest extends PhasesTest {

  public BadReferencesTest() {
    super("/examples/parseButFailCompile/multipleReferences",
        List.of("alpha", "beta", "fails.to.compile", "another.reference.module", "main.reference.module"));
  }


  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.REFERENCE_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertEquals(25, numberOfErrors);
  }
}
