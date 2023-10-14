package org.ek9lang.compiler.phase2;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests duplicate methods/operators on constructs.
 */
class BadDuplicateOperationsTest extends PhasesTest {

  public BadDuplicateOperationsTest() {
    super("/examples/parseButFailCompile/badDuplicateOperations");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.duplicate.classmethods").isEmpty());
    assertFalse(program.getParsedModules("bad.duplicate.traitmethods").isEmpty());
    assertFalse(program.getParsedModules("bad.duplicate.recordmethods").isEmpty());
    assertFalse(program.getParsedModules("bad.duplicate.servicemethods").isEmpty());
    assertFalse(program.getParsedModules("bad.duplicate.componentmethods").isEmpty());
    assertFalse(program.getParsedModules("bad.duplicate.recordoperators").isEmpty());
  }
}
