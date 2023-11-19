package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests duplicate methods/operators on constructs.
 */
class BadDuplicateOperationsTest extends PhasesTest {

  public BadDuplicateOperationsTest() {
    super("/examples/parseButFailCompile/badDuplicateOperations",
        List.of("bad.duplicate.classmethods",
            "bad.duplicate.traitmethods",
            "bad.duplicate.recordmethods",
            "bad.duplicate.servicemethods",
            "bad.duplicate.componentmethods",
            "bad.duplicate.recordoperators",
            "bad.name.collisions1",
            "bad.name.collisions2"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);


  }
}
