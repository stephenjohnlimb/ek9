package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for generic/template type operator constraints in POST_RESOLUTION_CHECKS phase.
 * Tests operator availability when generic types are parameterized with types that lack required operators.
 *
 * <p>Test corpus: fuzzCorpus/genericOperatorConstraints
 * Covers errors including:
 * - OPERATOR_NOT_DEFINED - Generic uses operator that parameterized type doesn't implement
 *
 * <p>Key Scenarios Tested:
 * 1. Explicit "constrain by Any" - Tests operators inherited from Any vs missing operators
 * 2. Operator Variety - Tests operators beyond the commonly tested ones (+, <=>, $$, ?)
 * 3. Multiple Operators - Generics requiring multiple operators, type missing some
 *
 * <p>Critical Language Semantics:
 * - All EK9 types implicitly extend Any, which provides 6 operators: ?, ==, $$, $, #?, <=>
 * - Unconstrained generic "of type T" can use ANY operator, validated at parameterization
 * - Constrained generic "of type T constrain by X" can only use operators from X
 * - When constrained by Any, only Any's 6 operators are available
 *
 * <p>Expected behavior:
 * - Compiler should NOT crash (robustness)
 * - Compilation should FAIL at POST_RESOLUTION_CHECKS phase when operator missing
 * - Specific OPERATOR_NOT_DEFINED errors reported for missing operators
 *
 * <p>Validates: Generic type parameterization operator constraint checking across EK9's operator set.
 *
 * <p>Gap Analysis: Existing tests cover +, <=>, $$, ? operators. This suite adds:
 * - Explicit "constrain by Any" scenarios (not previously tested)
 * - Operator variety: *, /, >, <, mod, ~ (previously untested in generics)
 * - Multiple simultaneous operator requirements
 */
class GenericOperatorConstraintsFuzzTest extends FuzzTestBase {

  public GenericOperatorConstraintsFuzzTest() {
    super("genericOperatorConstraints", CompilationPhase.POST_RESOLUTION_CHECKS, false);
  }

  @Test
  void testGenericOperatorConstraintsRobustness() {
    assertTrue(runTests() != 0);
  }
}
