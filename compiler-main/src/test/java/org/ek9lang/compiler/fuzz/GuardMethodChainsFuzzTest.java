package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for guard expressions with method chains.
 * Tests patterns like: if x <- obj.a().b().c()
 */
class GuardMethodChainsFuzzTest extends FuzzTestBase {

  public GuardMethodChainsFuzzTest() {
    super("guardMethodChains", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testGuardMethodChainsRobustness() {
    assertTrue(runTests() != 0);
  }
}
