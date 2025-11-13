package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for EK9 service validation Phase 1 (SYMBOL_DEFINITION).
 * <p>
 * Tests service error detection during symbol definition including:
 * - Service URI validation (path variables not supported at service level)
 * - HTTP operator validation (only +, +=, -, -=, :^:, :~:, ? supported)
 * - HTTP access verb context (not allowed in constructors/regular methods)
 * <p>
 * Current Coverage: 7 Phase 1 tests covering 3 SERVICE error types.
 */
class ServiceValidationPhase1FuzzTest extends FuzzTestBase {
  public ServiceValidationPhase1FuzzTest() {
    super("serviceValidation/phase1", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testServicePhase1ValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
