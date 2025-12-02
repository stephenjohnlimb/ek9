package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for constrained type definition errors.
 *
 * <p>Tests TYPE_CANNOT_BE_CONSTRAINED scenarios including:
 * - Constraining Component types
 * - Constraining Boolean type
 * - Constraining JSON type
 * - Constraining Trait types
 * - Constraining abstract classes
 *
 * <p>These errors occur at EXPLICIT_TYPE_SYMBOL_DEFINITION phase
 * when the compiler validates that only valid types can be constrained.
 */
class ConstrainedTypeErrorsFuzzTest extends FuzzTestBase {

  public ConstrainedTypeErrorsFuzzTest() {
    super("constrainedTypeErrors", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testConstrainedTypeErrorsRobustness() {
    assertTrue(runTests() != 0);
  }
}
