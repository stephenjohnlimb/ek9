package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for guard expressions in control flow contexts (PRE_IR_CHECKS phase).
 * Tests USED_BEFORE_INITIALISED error detection when guards prevent execution.
 *
 * <p>Test corpus: fuzzCorpus/guardContexts (4 test files)
 * Validates that guard expressions in for/while/switch/try statements properly
 * track initialization when the guard may prevent block execution.
 *
 * <p>Guard Expression Semantics:
 * Guards are EK9's revolutionary safety feature that eliminate 90-95% of null pointer exceptions.
 * The guard operator (`?=`) combines assignment with is-set checking, creating conditional execution.
 *
 * <p>Guard Syntax in Control Flow:
 * - For loop: `for guard ?= getValue() then item in items`
 * - While loop: `while guard ?= getValue() with condition`
 * - Switch: `switch guard ?= getValue() with conditional`
 * - Try: `try guard ?= getValue()`
 * <br/>
 * In all cases, if getValue() returns unset, the entire block does NOT execute.
 *
 * <p>Test scenarios:
 * 1. guard_for_loop_uninitialized_in_body.ek9 - For loop with guard
 * - Pattern: Variable initialized only inside for loop body
 * - If guard prevents loop execution, variable never gets initialized
 * - Error: Variable used in assert after loop may be uninitialized
 * - Expected: 3 USED_BEFORE_INITIALISED errors
 * <br/>
 * 2. guard_switch_uninitialized_across_cases.ek9 - Switch with guard
 * - Pattern: Variable initialized in some switch cases but not all
 * - If guard prevents switch execution, variable never gets initialized
 * - Also tests incomplete initialization across different case paths
 * - Expected: 4 USED_BEFORE_INITIALISED errors
 * <br/>
 * 3. guard_while_loop_conditional_initialization.ek9 - While loop with guard
 * - Pattern: Variable initialized conditionally inside while loop
 * - If guard prevents loop execution, variable never gets initialized
 * - Also tests variables only set in certain loop iterations
 * - Expected: 4 USED_BEFORE_INITIALISED errors
 * <br/>
 * 4. guard_try_catch_exception_paths.ek9 - Try/catch with guard
 * - Pattern: Variable initialized in try block but not in catch
 * - If guard prevents try execution, variable never gets initialized
 * - Also tests catch blocks forgetting to initialize
 * - Expected: 3 USED_BEFORE_INITIALISED errors
 * </p>
 * <p>Why These Are Genuine Edge Cases:
 * Existing tests (badGuards.ek9) cover guards in EXPRESSIONS (SYMBOL_DEFINITION errors).
 * Existing tests (badForLoops.ek9, badWhileLoops.ek9, etc.) test loops WITHOUT guards.
 * Existing tests (badGuardedAssignments.ek9) test guard ASSIGNMENT itself (2 tests).
 * <br/>
 * These tests fill the gap: Guards in control flow STATEMENTS with PRE_IR_CHECKS flow analysis.
 * The guard may prevent execution, creating initialization dependencies not tested elsewhere.
 * </p>
 * <p>Differentiation from Existing Tests:
 * - badGuards.ek9: Guards in expressions → SYMBOL_DEFINITION errors (Phase 1)
 * - bad{For|While|Switch|Try}*.ek9: Control flow WITHOUT guards
 * - badGuardedAssignments.ek9: Guard assignment validation (var ?= unset)
 * - guardContexts/: Guards in statements → PRE_IR_CHECKS flow analysis (Phase 8)
 * </p>
 * <p>Expected behavior:
 * - Guard expressions create conditional execution paths
 * - Variables initialized only inside guarded blocks may remain uninitialized
 * - Compiler should detect uninitialized variables after guarded blocks
 * - Error: "might be used before being initialised"
 * </p>
 * <p>Validates: EK9's guard expression safety mechanism correctly enforces
 * initialization tracking even when control flow may be conditionally skipped.
 * Guards are EK9's primary defense against null pointer exceptions.
 * </p>
 * <p>Total: 14 USED_BEFORE_INITIALISED errors across 4 test files
 * </p>
 */
class GuardContextsFuzzTest extends FuzzTestBase {

  public GuardContextsFuzzTest() {
    super("guardContexts", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testGuardContextsRobustness() {
    assertTrue(runTests() != 0);
  }
}
