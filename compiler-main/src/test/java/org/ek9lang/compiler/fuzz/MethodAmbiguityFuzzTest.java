package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for method and operator ambiguity scenarios in FULL_RESOLUTION phase.
 * Tests cost-based resolution when multiple methods/operators match with equal costs.
 *
 * <p>Test corpus: fuzzCorpus/methodAmbiguity (2 test files)
 * Covers METHOD_AMBIGUOUS error type with trait-based ambiguity.
 *
 * <p>Test scenarios:
 * 1. method_trait_ambiguity.ek9 - Method overloading with trait ambiguity
 * - Two traits: T1, T2 (both have getValue() method)
 * - Class C implements BOTH traits (T1 and T2)
 * - TestClass has two overloaded checkMethod():
 * - checkMethod(arg as T1) → returns 10
 * - checkMethod(arg as T2) → returns 20
 * - Call: test.checkMethod(c) where c is type C creates ambiguity:
 * - Method 1: C→T1 (0.10 trait) = 0.10 total
 * - Method 2: C→T2 (0.10 trait) = 0.10 total
 * - Equal costs within 0.001 tolerance → METHOD_AMBIGUOUS
 * <br/>
 * 2. operator_trait_ambiguity.ek9 - Operator overloading with trait ambiguity
 * - Same pattern as test 1 but with operator + instead of method
 * - TestClass has two overloaded operator +:
 * - operator +(arg as T1) → returns 10
 * - operator +(arg as T2) → returns 20
 * - Call: test + c where c is type C creates ambiguity:
 * - Operator 1: C→T1 (0.10 trait) = 0.10 total
 * - Operator 2: C→T2 (0.10 trait) = 0.10 total
 * - Equal costs within 0.001 tolerance → METHOD_AMBIGUOUS
 * </p>
 * <p>Method Resolution Cost Algorithm:
 * - ZERO_COST (0.0): Exact type match
 * - SUPER_COST (0.05): Match via superclass (+0.05 per level)
 * - TRAIT_COST (0.10): Match via trait (+0.10 per trait)
 * - COERCION_COST (0.5): Type coercion via #^ operator
 * - HIGH_COST (20.0): 'Any' type match (last resort)
 * - Ambiguity occurs when costs are within 0.001 tolerance
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect METHOD_AMBIGUOUS error for BOTH methods and operators
 * - Compilation should FAIL at FULL_RESOLUTION phase
 * - Error message should indicate ambiguous method/operator resolution
 * </p>
 * <p>Validates: Both methods and operators correctly detect ambiguity with trait-based matching.
 * Gap addressed: Only 2 existing METHOD_AMBIGUOUS tests (twoBadExample, threeBadExample),
 * both using inheritance hierarchy and coercion. These tests cover trait-based ambiguity.
 * </p>
 * <p>Bug fix: operator_trait_ambiguity.ek9 revealed a compiler bug where operators
 * failed to check for ambiguity before reporting OPERATOR_NOT_DEFINED. The fix adds
 * an ambiguity check in RequiredOperatorPresentOrError.java to match the behavior
 * of ResolveMethodOrError.java and TypeConstraintOrError.java.
 * </p>
 * <p>Note: Limited test count (2 files) reflects:
 * - twoBadExample.ek9 already covers inheritance hierarchy ambiguity
 * - threeBadExample.ek9 already covers coercion ambiguity
 * - These tests add trait-based ambiguity (new coverage)
 * - Functions cannot be overloaded (DUPLICATE_SYMBOL error)
 * - Generic types are monomorphized before resolution (no ambiguity)
 * - 'Any' type uses HIGH_COST to prevent equal matches
 * </p>
 */
class MethodAmbiguityFuzzTest extends FuzzTestBase {

  public MethodAmbiguityFuzzTest() {
    super("methodAmbiguity", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testMethodAmbiguityRobustness() {
    assertTrue(runTests() != 0);
  }
}
