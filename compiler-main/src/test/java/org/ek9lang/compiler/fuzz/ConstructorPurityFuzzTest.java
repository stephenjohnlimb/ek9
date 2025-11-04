package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for constructor purity constraint errors in FULL_RESOLUTION phase.
 * Tests MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS constraint.
 *
 * <p>Test corpus: fuzzCorpus/constructorPurity (2 test files)
 * Validates that all constructors in a class/record must have consistent purity.
 *
 * <p>Test scenarios:
 * 1. constructor_purity_three_mixed.ek9 - Three constructors with mixed purity
 * - Pattern: 1 non-pure default, 2 pure constructors
 * - Error: Constructors must all be pure or all non-pure
 * - Tests MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS (existing coverage: 2 tests)
 * <br/>
 * 2. constructor_purity_record.ek9 - Record with mixed constructor purity
 * - Pattern: 1 non-pure default, 1 pure constructor in record
 * - Error: Record constructors must have consistent purity
 * - Tests MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS
 * </p>
 * <p>Type Constraint Semantics:
 * - MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS: All constructors must have same purity
 * - Error detected at FULL_RESOLUTION phase during constructor validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect purity constraint violations at FULL_RESOLUTION phase
 * - Constructor purity must be consistent across all constructors
 * </p>
 * <p>Validates: Constructor purity enforcement correctly prevents inconsistent
 * purity declarations across multiple constructors.
 * </p>
 * <p>Gap addressed: Critical type constraint errors had minimal coverage:
 * - MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS: 2 existing tests → 4 total tests
 * This ensures EK9's type system correctly enforces this critical constraint.
 * </p>
 */
class ConstructorPurityFuzzTest extends FuzzTestBase {

  public ConstructorPurityFuzzTest() {
    super("constructorPurity", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testConstructorPurityRobustness() {
    assertTrue(runTests() != 0);
  }
}
