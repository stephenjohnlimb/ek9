package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for complex coalescing operator chains.
 * Tests deep chains and mixed coalescing operators.
 */
class ComplexCoalescingChainsFuzzTest extends FuzzTestBase {

  public ComplexCoalescingChainsFuzzTest() {
    super("complexCoalescingChains", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testComplexCoalescingChainsRobustness() {
    assertTrue(runTests() != 0);
  }
}
