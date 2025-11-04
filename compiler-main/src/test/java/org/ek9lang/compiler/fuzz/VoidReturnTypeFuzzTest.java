package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for Void return type meaningless errors in FULL_RESOLUTION phase.
 * Tests RETURN_TYPE_VOID_MEANINGLESS constraint.
 *
 * <p>Test corpus: fuzzCorpus/voidReturnType (2 test files)
 * Validates that Void-returning functions/methods cannot be assigned to variables.
 *
 * <p>Test scenarios:
 * 1. void_function_assignment.ek9 - Assign Void function result to variable
 * - Pattern: Function with no return value assigned to variable
 * - Error: Void return type cannot be used with assignment
 * - Tests RETURN_TYPE_VOID_MEANINGLESS (existing coverage: 2 tests)
 * <br/>
 * 2. void_method_assignment.ek9 - Assign Void method result to variable
 * - Pattern: Method with no return value assigned to variable
 * - Error: Void assignments are meaningless
 * - Tests RETURN_TYPE_VOID_MEANINGLESS
 * </p>
 * <p>Return Type Constraint Semantics:
 * - RETURN_TYPE_VOID_MEANINGLESS: Functions and methods that return Void (no value)
 *   cannot have their results assigned to variables since there is no value to assign
 * - Error detected at FULL_RESOLUTION phase during assignment validation
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect Void return assignments at FULL_RESOLUTION phase
 * - Only functions/methods with actual return values can be assigned
 * - Void-returning operations must be used as statements, not expressions
 * </p>
 * <p>Validates: Void return type enforcement prevents meaningless assignments
 * and ensures proper distinction between statements and expressions.
 * </p>
 * <p>Gap addressed: Void return errors had minimal coverage:
 * - RETURN_TYPE_VOID_MEANINGLESS: 2 existing tests → 4 total tests
 * Covers function and method Void return assignments.
 * This ensures EK9 correctly prevents meaningless Void assignments.
 * </p>
 */
class VoidReturnTypeFuzzTest extends FuzzTestBase {

  public VoidReturnTypeFuzzTest() {
    super("voidReturnType", CompilationPhase.FULL_RESOLUTION);
  }

  @Test
  void testVoidReturnTypeRobustness() {
    assertTrue(runTests() != 0);
  }
}
