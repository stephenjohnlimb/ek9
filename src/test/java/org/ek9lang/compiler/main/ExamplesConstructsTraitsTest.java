package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just test traits all compile.
 */
class ExamplesConstructsTraitsTest extends PhasesTest {

  public ExamplesConstructsTraitsTest() {
    super("/examples/constructs/traits", false, false);
  }


  @Test
  void testPhaseDevelopment() {
    //TODO correct errors:
    //TODO 'SimpleProcessor' on line 63 position 4: 'public Boolean <- lowCost()' on 'Processor' and 'Processor': conflicting methods to be resolved
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
