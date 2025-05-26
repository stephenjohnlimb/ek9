package org.ek9lang.compiler.phase4;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Just tests generic classes with constraints and in this case where a constraint does not make the type
 * have a super of the constraining type.
 */
class BadGenericConstraintUseTest extends PhasesTest {

  public BadGenericConstraintUseTest() {
    super("/examples/parseButFailCompile/badGenericConstraintUse",
        List.of("net.customer.constrained"));
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.POST_RESOLUTION_CHECKS);
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult, final int numberOfErrors,
                                    final CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
