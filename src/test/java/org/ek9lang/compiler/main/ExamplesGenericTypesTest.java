package org.ek9lang.compiler.main;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

class ExamplesGenericTypesTest extends PhasesTest {

  public ExamplesGenericTypesTest() {
    super("/examples/genericTypes");
  }

  @Test
  void testPhasedDevelopment() {
    //TODO fix up code there is a stack overflow here hashCode again.
    //TODO Plus there are some other EK9 code mistakes.
    //Also some missing methods in stock EK9 lib
    testToPhase(CompilationPhase.TYPE_HIERARCHY_CHECKS);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertTrue(compilationResult);
    assertEquals(0, numberOfErrors);
  }
}
