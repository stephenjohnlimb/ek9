package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for variable resolution errors in FULL_RESOLUTION phase.
 * Tests declaration ordering, self-assignment detection, and variable usage validation.
 *
 * <p>Test corpus: fuzzCorpus/variableResolution (4 test files)
 * Covers critical variable resolution error types with minimal existing coverage.
 *
 * <p>Test scenarios:
 * 1. used_before_defined_function.ek9 - Forward references in function scope
 * - Variable used before declaration: result <- undefinedVar + 10
 * - Expression with forward reference: calculated <- (notYetDefined * 2) + 5
 * - Assignment right-side forward reference: y <- x + futureVariable
 * - Tests USED_BEFORE_DEFINED error (existing coverage: 1 test)
 * <br/>
 * 2. used_before_defined_class.ek9 - Forward references in class methods
 * - Method-local variable used before declaration
 * - Complex nested expression with forward reference
 * - Tests USED_BEFORE_DEFINED in aggregate context
 * <br/>
 * 3. self_assignment_variants.ek9 - Self-assignment with different operators
 * - Self assignment with := operator: x := x
 * - Self assignment with :=: (copy) operator: value :=: value
 * - Self assignment with :=? (guarded) operator: count :=? count
 * - Self assignment in nested scope
 * - Tests SELF_ASSIGNMENT error (existing coverage: 4 tests in 1 file)
 * <br/>
 * 4. not_referenced_scopes.ek9 - Unreferenced variables across scopes
 * - Unreferenced variable in function scope
 * - Unreferenced variable in nested scope
 * - Multiple unreferenced variables in same function
 * - Unreferenced local variable in class method
 * - Tests NOT_REFERENCED error (existing coverage: 4 tests in 2 files)
 * </p>
 * <p>Variable Resolution Semantics:
 * - USED_BEFORE_DEFINED: Variable referenced textually before declaration in same scope
 * - SELF_ASSIGNMENT: Variable assigned to itself (usually logic error)
 * - NOT_REFERENCED: Variable declared but never used (dead code indicator)
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect variable resolution errors at FULL_RESOLUTION phase
 * - USED_BEFORE_DEFINED errors prevent reading uninitialized variables
 * - SELF_ASSIGNMENT warnings catch likely logic errors
 * - NOT_REFERENCED warnings identify dead code
 * </p>
 * <p>Validates: Variable declaration ordering and usage tracking are correctly
 * enforced across functions, methods, and control flow constructs.
 * </p>
 * <p>Gap addressed: Critical variable resolution errors had minimal coverage:
 * - USED_BEFORE_DEFINED: 1 existing test → 6 new tests (functions + class methods)
 * - SELF_ASSIGNMENT: 4 tests in 1 file → 4 new tests (all assignment operators)
 * - NOT_REFERENCED: 4 tests in 2 files → 4 new tests (diverse scopes)
 * These errors are critical for EK9's initialization safety and dead code detection.
 * </p>
 */
class VariableResolutionFuzzTest extends FuzzTestBase {

  public VariableResolutionFuzzTest() {
    super("variableResolution", CompilationPhase.FULL_RESOLUTION, false);
  }

  @Test
  void testVariableResolutionRobustness() {
    assertTrue(runTests() != 0);
  }
}
