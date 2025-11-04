package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for Boolean return type requirement errors in FULL_RESOLUTION phase.
 * Tests MUST_RETURN_BOOLEAN constraint.
 *
 * <p>Test corpus: fuzzCorpus/booleanReturnType (2 test files)
 * Validates that filter operations require Boolean-returning functions.
 *
 * <p>Test scenarios:
 * 1. filter_returns_integer.ek9 - Filter function returns Integer instead of Boolean
 * - Pattern: Stream filter with function returning Integer
 * - Error: Filter operations require Boolean return type
 * - Tests MUST_RETURN_BOOLEAN (existing coverage: 2 tests)
 * <br/>
 * 2. filter_returns_string.ek9 - Filter function returns String instead of Boolean
 * - Pattern: Stream filter with function returning String
 * - Error: Filter requires Boolean, not String
 * - Tests MUST_RETURN_BOOLEAN
 * </p>
 * <p>Return Type Constraint Semantics:
 * - MUST_RETURN_BOOLEAN: Filter operations in stream pipelines require predicates
 *   that return Boolean values for conditional evaluation
 * - Error detected at FULL_RESOLUTION phase during stream pipeline validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect non-Boolean return types in filter operations at FULL_RESOLUTION
 * - Only Boolean-returning functions can be used as filter predicates
 * - Type checking ensures predicates are boolean expressions
 * </p>
 * <p>Validates: Boolean return type enforcement ensures stream filter operations
 * have proper predicate semantics, preventing runtime type errors.
 * </p>
 * <p>Gap addressed: Boolean return errors had minimal coverage:
 * - MUST_RETURN_BOOLEAN: 2 existing tests → 4 total tests
 * Covers Integer and String return types in filter operations.
 * This ensures EK9's stream pipeline system correctly enforces Boolean requirements.
 * </p>
 */
class BooleanReturnTypeFuzzTest extends FuzzTestBase {

  public BooleanReturnTypeFuzzTest() {
    super("booleanReturnType", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testBooleanReturnTypeRobustness() {
    assertTrue(runTests() != 0);
  }
}
