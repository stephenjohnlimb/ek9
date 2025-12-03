package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for 'allow only' trait restriction.
 * Tests unresolved types, invalid genus, and multiple restrictions.
 */
class AllowOnlyRestrictionFuzzTest extends FuzzTestBase {

  public AllowOnlyRestrictionFuzzTest() {
    super("allowOnlyRestriction", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testAllowOnlyRestrictionRobustness() {
    assertTrue(runTests() != 0);
  }
}
