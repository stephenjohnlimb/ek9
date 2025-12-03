package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for combined guard and coalescing operator patterns.
 * Tests complex combinations that developers might naturally write.
 */
class GuardCoalescingCombinedFuzzTest extends FuzzTestBase {

  public GuardCoalescingCombinedFuzzTest() {
    super("guardCoalescingCombined", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testGuardCoalescingCombinedRobustness() {
    assertTrue(runTests() != 0);
  }
}
