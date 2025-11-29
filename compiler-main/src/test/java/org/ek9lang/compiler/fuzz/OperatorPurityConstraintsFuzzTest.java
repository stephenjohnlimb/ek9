package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for operator purity constraint validation.
 * Tests OPERATOR_MUST_BE_PURE and OPERATOR_CANNOT_BE_PURE constraints.
 *
 * <p>Test corpus: fuzzCorpus/operatorPurityConstraints (3 test files)
 * Validates that operators have correct purity declarations.
 *
 * <p>Test scenarios:
 * 1. operators_must_be_pure.ek9 - matches operator without pure
 * - Pattern: matches operator without pure modifier
 * - Error: matches doesn't mutate state, must be pure
 * - Tests OPERATOR_MUST_BE_PURE (1 error)
 * - Note: contains already tested in operatorMisuse/contains_not_pure.ek9
 * <br/>
 * 2. operators_cannot_be_pure.ek9 - Mutating operators marked pure
 * - Pattern: :=:, :~:, :^: operators marked as pure
 * - Error: Copy/merge/replace operators mutate state
 * - Tests OPERATOR_CANNOT_BE_PURE (3 errors)
 * <br/>
 * 3. compound_operators_cannot_be_pure.ek9 - Compound assignment operators
 * - Pattern: +=, -=, *=, /= operators marked as pure
 * - Error: Compound operators inherently mutate state
 * - Tests OPERATOR_CANNOT_BE_PURE (4 errors)
 * </p>
 * <p>Operator Purity Semantics:
 * - Query operators (contains, matches, ==, etc.): Must be pure (no side effects)
 * - Mutation operators (:=:, :~:, :^:, +=, etc.): Cannot be pure (they mutate)
 * - This ensures semantic clarity and enables optimization
 * - Error detected at EXPLICIT_TYPE_SYMBOL_DEFINITION phase
 * </p>
 * <p>Gap addressed: Operator purity constraints had minimal coverage:
 * - OPERATOR_MUST_BE_PURE: 1 existing test → 2 total (matches is new)
 * - OPERATOR_CANNOT_BE_PURE: 1 existing test → 8 total
 * Covers matches query operator, copy/merge/replace, and compound assignment.
 * </p>
 */
class OperatorPurityConstraintsFuzzTest extends FuzzTestBase {

  public OperatorPurityConstraintsFuzzTest() {
    super("operatorPurityConstraints", CompilationPhase.EXPLICIT_TYPE_SYMBOL_DEFINITION);
  }

  @Test
  void testOperatorPurityConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}
