package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Deals with misuse of operators when being defined.
 */
class OperatorMisuseFuzzTest extends FuzzTestBase {

  public OperatorMisuseFuzzTest() {
    super("operatorMisuse", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testOperatorValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
