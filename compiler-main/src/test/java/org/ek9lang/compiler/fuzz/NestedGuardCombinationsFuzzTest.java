package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for nested guard combinations.
 * Tests 3+ deep nesting and mixed control flow guards.
 */
class NestedGuardCombinationsFuzzTest extends FuzzTestBase {

  public NestedGuardCombinationsFuzzTest() {
    super("nestedGuardCombinations", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testNestedGuardCombinationsRobustness() {
    assertTrue(runTests() != 0);
  }
}
