package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for generic type resolution errors.
 *
 * <p>Tests TYPE_NOT_RESOLVED errors when generics are parameterized with
 * non-existent, misspelled, or invalid types.
 *
 * <p>These errors occur at EXPLICIT_TYPE_SYMBOL_DEFINITION phase (phase 4)
 * when type resolution fails for generic type arguments.
 */
class GenericTypeResolutionErrorsFuzzTest extends FuzzTestBase {

  public GenericTypeResolutionErrorsFuzzTest() {
    super("genericTypeResolutionErrors", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testGenericTypeResolutionErrorsRobustness() {
    assertTrue(runTests() != 0);
  }
}
