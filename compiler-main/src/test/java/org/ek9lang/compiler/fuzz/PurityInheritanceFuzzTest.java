package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for purity inheritance constraint validation.
 * Tests SUPER_IS_PURE, SUPER_IS_NOT_PURE, MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS,
 * NONE_PURE_CALL_IN_PURE_SCOPE, NO_PURE_REASSIGNMENT.
 *
 * <p>Test corpus: fuzzCorpus/purityInheritance (6 test files)
 * Validates purity constraints in class and function hierarchies.
 *
 * <p>Test scenarios:
 * 1. pure_super_not_pure_child.ek9 - Pure super with non-pure child
 * - Pattern: Pure function super, child without 'as pure'
 * - Tests SUPER_IS_PURE (2 errors)
 * <br/>
 * 2. not_pure_super_pure_child.ek9 - Non-pure super with pure child
 * - Pattern: Non-pure function super, child marked 'as pure'
 * - Tests SUPER_IS_NOT_PURE (2 errors)
 * <br/>
 * 3. mixed_constructor_purity.ek9 - Mixed constructor purity
 * - Pattern: Some constructors pure, others not
 * - Tests MIX_OF_PURE_AND_NOT_PURE_CONSTRUCTORS (3 errors)
 * <br/>
 * 4. pure_calls_not_pure.ek9 - Pure calls non-pure
 * - Pattern: Pure function body calls non-pure function
 * - Tests NONE_PURE_CALL_IN_PURE_SCOPE (2 errors)
 * <br/>
 * 5. function_purity_mismatch.ek9 - Function purity mismatch
 * - Pattern: Function extends with different purity
 * - Tests SUPER_IS_PURE, SUPER_IS_NOT_PURE (2 errors)
 * <br/>
 * 6. pure_reassignment_error.ek9 - Pure reassignment
 * - Pattern: Direct assignment in pure context
 * - Tests NO_PURE_REASSIGNMENT (2 errors)
 * </p>
 * <p>Purity Semantics:
 * - Pure functions guarantee no side effects
 * - Purity must be consistent in inheritance hierarchy
 * - All constructors must match in purity
 * - Pure code cannot call non-pure code
 * - Error detected at FULL_RESOLUTION phase
 * </p>
 * <p>Gap addressed: Purity constraints had examples but thin fuzz coverage:
 * - Tests focus on specific purity violation patterns
 * - Each file targets distinct violation category
 * - 13 total error scenarios across 6 files
 * </p>
 */
class PurityInheritanceFuzzTest extends FuzzTestBase {

  public PurityInheritanceFuzzTest() {
    super("purityInheritance", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testPurityInheritanceRobustness() {
    assertTrue(runTests() != 0);
  }
}
