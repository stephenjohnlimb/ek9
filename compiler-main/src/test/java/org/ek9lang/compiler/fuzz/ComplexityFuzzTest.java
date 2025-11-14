package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for cyclomatic complexity errors in PRE_IR_CHECKS phase.
 * Tests EXCESSIVE_COMPLEXITY error detection.
 *
 * <p>Test corpus: fuzzCorpus/complexity (5 test files)
 * Validates that complexity analysis correctly detects functions/operators/dynamic functions/classes
 * exceeding cyclomatic complexity thresholds.
 *
 * <p>Complexity Thresholds (from AcceptableConstructComplexityOrError.java):
 * - Functions/Methods/Operators: Maximum complexity = 50
 * - Types/Classes: Maximum complexity = 500
 *
 * <p>Complexity Calculation (from ComplexityCounter.java):
 * Base complexity:
 * - Function/Method/Operator: +1
 * - Dynamic function: +2
 * - Class/Record/Trait: +1
 * - Service/Component/Application: +2
 * - Dynamic class: +2
 * <br/>
 * Incremental complexity:
 * - Uninitialized variable (?): +1 each
 * - Conditional assignment (:=?): +1
 * - Guard expression: +1
 * - is-set check (?): +1 each
 * - if statement: +1
 * - case expression (switch): +1 per case
 * - try block: +1
 * - catch block: +1
 * - finally block: +1
 * - throw statement: +2
 * - for loop: +1
 * - for loop with 'by' clause: +2
 * - while loop: +1
 * - do-while loop: +1
 * - Pipeline part (|): +1 each
 * - Stream termination (collect, etc.): +1
 * - Stream cat: +1
 * - Comparison operators (<, >, ==, etc.): +1 each
 * - Boolean logic operators (and/or on Boolean type): +1 each (short-circuit branching)
 * - Bitwise operators (and/or on Bits type): +0 (no branching)
 * - Arguments: 3-4 args = +1, 5+ args = +2
 * - Multiple resources in try: +2
 * </p>
 * <p>Test scenarios:
 * 1. boundary_function_complexity_fail.ek9 - Function with EXACTLY 51 complexity (1 over threshold)
 * - Pattern: Combination of uninitialized vars, is-set checks, conditionals, if statements
 * - Error: Function exceeds maximum complexity of 50 by 1
 * - Validates boundary enforcement at complexity = 51
 * - Expected: 1 EXCESSIVE_COMPLEXITY error
 * <br/>
 * 2. comparison_operator_explosion.ek9 - Many comparison operators
 * - Pattern: 48 if statements, each with a comparison operator
 * - Each if statement (+1) and each comparison operator (+1)
 * - Total: 103 complexity from repeated if/comparison patterns
 * - Validates detection of high complexity from operator-heavy code
 * - Expected: 1 EXCESSIVE_COMPLEXITY error
 * <br/>
 * 3. excessive_operator_complexity.ek9 - Operator with excessive complexity
 * - Pattern: Comparison operator (<=>)with many is-set checks, conditionals, and comparisons
 * - Total: 71 complexity (21 over threshold)
 * - Validates EXCESSIVE_COMPLEXITY detection on operators (not just functions)
 * - Expected: 1 EXCESSIVE_COMPLEXITY error
 * <br/>
 * 4. excessive_dynamic_function_complexity.ek9 - Dynamic function with excessive complexity
 * - Pattern: Dynamic function (base complexity 2) with captured variables and complex logic
 * - Dynamic function: 71 complexity, wrapper function: 73 complexity
 * - Validates complexity flow upward (dynamic → containing function)
 * - Expected: 2 EXCESSIVE_COMPLEXITY errors (dynamic function + wrapper)
 * <br/>
 * 5. stream_pipeline_complexity.ek9 - Stream pipeline with excessive complexity
 * - Pattern: Stream pipeline combined with conditional logic (if statements)
 * - Total: 56 complexity (6 over threshold)
 * - Validates that stream operations (cat, pipes, collect) count toward overall complexity
 * - Expected: 1 EXCESSIVE_COMPLEXITY error
 * </p>
 * <p>Complexity Semantics:
 * - EXCESSIVE_COMPLEXITY: Functions/methods/operators exceeding 50 complexity threshold
 * - Classes exceeding 500 complexity threshold (not tested here)
 * - Complexity flows upward: method complexity adds to containing class complexity
 * - Error appears on construct declaration line (function/class signature)
 * - Detected at PRE_IR_CHECKS phase during complexity analysis
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect functions exceeding 50 complexity threshold
 * - Functions at exactly 50 complexity should pass (boundary condition)
 * - Functions at 51+ complexity should trigger EXCESSIVE_COMPLEXITY error
 * - Complexity calculation should account for all control flow constructs
 * </p>
 * <p>Validates: Cyclomatic complexity analysis correctly enforces code quality
 * thresholds across different complexity patterns (boolean logic, loops, streams).
 * </p>
 * <p>Gap addressed: EXCESSIVE_COMPLEXITY error type had ZERO fuzz test coverage.
 * Existing comprehensive tests in examples/parseAndCompile/complexity/ cover basic
 * scenarios (function at 54, class at 549, argument count limit). These fuzz tests
 * add boundary condition validation and pattern-based complexity testing:
 * - Boundary condition: exactly 51 complexity (1 over threshold) triggers error
 * - Comparison operator explosion (103 complexity from repeated if/comparison patterns)
 * - Operator complexity: <=> operator at 71 complexity (validates operators, not just functions)
 * - Dynamic function complexity: validates base complexity 2 and upward flow (dynamic → wrapper)
 * <br/>
 * Boolean Logic Complexity: The compiler correctly distinguishes between:
 * - Boolean and/or operators: Add complexity (short-circuit evaluation creates branching)
 * - Bits and/or operators: Do NOT add complexity (bitwise operations, no branching)
 * This type-aware complexity counting is implemented via FormOfBooleanLogic.java in PreIRListener.
 * <br/>
 * Total: 6 EXCESSIVE_COMPLEXITY errors across 5 test files
 * </p>
 */
class ComplexityFuzzTest extends FuzzTestBase {

  public ComplexityFuzzTest() {
    super("complexity", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testComplexityRobustness() {
    assertTrue(runTests() != 0);
  }
}
