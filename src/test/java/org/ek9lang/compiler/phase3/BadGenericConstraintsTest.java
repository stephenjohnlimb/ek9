package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests generic classes with constraints.
 */
class BadGenericConstraintsTest extends PhasesTest {

  public BadGenericConstraintsTest() {
    super("/examples/parseButFailCompile/badGenericConstraints",
        List.of("bad.generic.class.constraints",
            "bad.generic.class.function.constraints",
            "functiondelegate.inrecord.withgeneric"), false, true);
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
