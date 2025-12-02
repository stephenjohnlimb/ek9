package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for generic type parameterization errors in SYMBOL_DEFINITION phase.
 * Tests GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT and TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION.
 *
 * <p>Test corpus: fuzzCorpus/genericParameterizationErrors
 * Validates that generic types and functions detect incorrect parameter counts.
 *
 * <p>Test scenarios:
 * 1. wrong_type_count_single.ek9 - Single-param generics given wrong parameter counts
 *    - Pattern: Box of T given (String, Integer) instead of just String
 *    - Error: GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT when count doesn't match
 *    - Error: TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION when used as type without params
 * </p>
 * <p>Constraint Semantics:
 * - GENERIC_TYPE_OR_FUNCTION_PARAMETERS_INCORRECT (E06020): Triggered when a generic type
 *   or function is parameterized with the wrong number of type arguments
 * - TEMPLATE_TYPE_REQUIRES_PARAMETERIZATION (E04080): Triggered when a generic type
 *   is used as a type (in signatures, fields) without providing type parameters
 * - Both errors detected at SYMBOL_DEFINITION phase
 * </p>
 * <p>Gap addressed:
 * - genericParameterizationErrors directory was EMPTY (0 tests)
 * - Now provides systematic coverage for parameter count mismatches
 * </p>
 */
class GenericParameterizationErrorsFuzzTest extends FuzzTestBase {

  public GenericParameterizationErrorsFuzzTest() {
    super("genericParameterizationErrors", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testGenericParameterizationErrorsRobustness() {
    assertTrue(runTests() != 0);
  }
}
