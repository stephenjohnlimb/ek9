package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for Result type constraint errors in SYMBOL_DEFINITION phase.
 * Tests RESULT_MUST_HAVE_DIFFERENT_TYPES constraint.
 *
 * <p>Test corpus: fuzzCorpus/resultTypeConstraints (2 test files)
 * Validates that Result<T1, T2> requires T1 != T2.
 *
 * <p>Test scenarios:
 * 1. result_nested_generic_same_type.ek9 - Result with nested generics of same type
 * - Pattern: Result of (List of String, List of String)
 * - Error: Both type parameters resolve to same generic type
 * - Tests RESULT_MUST_HAVE_DIFFERENT_TYPES (existing coverage: 2 tests)
 * <br/>
 * 2. result_same_type_integer.ek9 - Result with Integer for both types
 * - Pattern: Result of (Integer, Integer)
 * - Error: Same primitive type for OK and Error
 * - Tests RESULT_MUST_HAVE_DIFFERENT_TYPES
 * </p>
 * <p>Type Constraint Semantics:
 * - RESULT_MUST_HAVE_DIFFERENT_TYPES: Result<T1, T2> requires T1 != T2
 * - Error detected at SYMBOL_DEFINITION phase during type parameterization
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect type constraint violations at SYMBOL_DEFINITION phase
 * - Result types must have distinct OK and Error types
 * </p>
 * <p>Validates: Type constraint enforcement correctly prevents invalid Result
 * parameterization with identical types.
 * </p>
 * <p>Gap addressed: Critical type constraint errors had minimal coverage:
 * - RESULT_MUST_HAVE_DIFFERENT_TYPES: 2 existing tests → 4 total tests
 * This ensures EK9's type system correctly enforces this critical constraint.
 * </p>
 */
class ResultTypeConstraintsFuzzTest extends FuzzTestBase {

  public ResultTypeConstraintsFuzzTest() {
    super("resultTypeConstraints", CompilationPhase.SYMBOL_DEFINITION);
  }

  @Test
  void testResultTypeConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}
