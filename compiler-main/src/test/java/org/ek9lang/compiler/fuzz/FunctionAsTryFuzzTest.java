package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for 'function' keyword as try alternative.
 * Tests function blocks, handle (catch), finally, and nested combinations.
 */
class FunctionAsTryFuzzTest extends FuzzTestBase {

  public FunctionAsTryFuzzTest() {
    super("functionAsTry", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testFunctionAsTryRobustness() {
    assertTrue(runTests() != 0);
  }
}
