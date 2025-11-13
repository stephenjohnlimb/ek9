package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for EK9 service validation Phase 2 (EXPLICIT_TYPE_SYMBOL_DEFINITION).
 * <p>
 * Tests service error detection during explicit type definition including:
 * - HTTP parameter validation (type compatibility, qualifier rules)
 * - HTTP header validation (must have qualifier name)
 * - Return type validation (must be HTTPResponse or compatible)
 * - Service method requirements (must have return declaration)
 * <p>
 * Current Coverage: 5 Phase 2 tests covering 4 SERVICE error types.
 */
class ServiceValidationPhase2FuzzTest extends FuzzTestBase {
  public ServiceValidationPhase2FuzzTest() {
    super("serviceValidation/phase2", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testServicePhase2ValidationRobustness() {
    assertTrue(runTests() != 0);
  }
}
