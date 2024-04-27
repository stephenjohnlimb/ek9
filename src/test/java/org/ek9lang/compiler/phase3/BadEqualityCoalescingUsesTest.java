package org.ek9lang.compiler.phase3;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.ek9lang.compiler.common.PhasesTest;
import org.junit.jupiter.api.Test;

/**
 * Tests equality coalescing operations, some good and some bad, detecting errors.
 */
class BadEqualityCoalescingUsesTest extends PhasesTest {

  public BadEqualityCoalescingUsesTest() {
    super("/examples/parseButFailCompile/equalityCoalescing",
        List.of("equality.coalescing"), false, true);
  }

  @Test
  void testPhaseDevelopment() {
    testToPhase(CompilationPhase.FULL_RESOLUTION);
  }

  @Override
  protected void assertFinalResults(boolean compilationResult, int numberOfErrors, CompilableProgram program) {
    assertFalse(compilationResult);
  }
}
