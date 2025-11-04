package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for operator validation errors in SYMBOL_DEFINITION phase.
 */
class OperatorConflictsFuzzTest extends FuzzTestBase {

  public OperatorConflictsFuzzTest() {
    super("operatorConflicts", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testOperatorValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
