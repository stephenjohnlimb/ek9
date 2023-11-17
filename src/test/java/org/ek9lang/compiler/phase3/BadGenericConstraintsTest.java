package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests generic classes with constraints.
 */
class BadGenericConstraintsTest extends PhasesTest {

  public BadGenericConstraintsTest() {
    super("/examples/parseButFailCompile/badGenericConstraints");
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
    assertFalse(program.getParsedModules("bad.generic.class.constraints").isEmpty());
    assertFalse(program.getParsedModules("bad.generic.class.function.constraints").isEmpty());
    assertFalse(program.getParsedModules("functiondelegate.inrecord.withgeneric").isEmpty());
  }
}
