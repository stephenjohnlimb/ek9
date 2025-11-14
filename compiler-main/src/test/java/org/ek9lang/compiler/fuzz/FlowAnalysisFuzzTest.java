package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for flow analysis errors in PRE_IR_CHECKS phase.
 * Tests USED_BEFORE_INITIALISED and RETURN_NOT_ALWAYS_INITIALISED errors.
 *
 * <p>Test corpus: fuzzCorpus/flowAnalysis (6 test files)
 * Validates that flow analysis correctly detects uninitialized variables in
 * expression contexts, string interpolation, recursive functions, type coercion,
 * operator precedence, and exception throwing.
 *
 * <p>Test scenarios:
 * 1. expression_context_initialization.ek9 - Uninitialized variables in expression contexts
 * - Pattern: Uninitialized variables used in arithmetic, comparison, logical expressions
 * - Error: Variables must be initialized before use in any expression context
 * - Tests USED_BEFORE_INITIALISED in:
 *   - Arithmetic expressions (value + initialized)
 *   - Comparison expressions (value > initialized)
 *   - Logical expressions (flag and otherFlag)
 *   - Method arguments (stdout.println(value))
 *   - Ternary expressions (condition? <- "yes" : "no")
 * - Expected: 5 USED_BEFORE_INITIALISED errors
 * <br/>
 * 2. text_interpolation_uninitialized.ek9 - Uninitialized variables in string interpolation
 * - Pattern: Uninitialized variables embedded in string interpolation expressions
 * - Error: Variables must be initialized before use in ${...} interpolation
 * - Tests USED_BEFORE_INITIALISED in:
 *   - Simple interpolation (`Hello ${name}!`)
 *   - Multi-variable interpolation (`${firstName} ${lastName}`)
 *   - Numeric interpolation (`Count: ${count} of ${total}`)
 *   - Complex interpolation (`User ${username} has ID ${userId}`)
 * - Expected: 4 USED_BEFORE_INITIALISED errors
 * <br/>
 * 3. recursive_function_initialization.ek9 - Incomplete initialization in recursive functions
 * - Pattern: Recursive functions with missing initialization in some execution paths
 * - Error: Return values must be initialized across ALL paths including base/recursive/edge cases
 * - Tests RETURN_NOT_ALWAYS_INITIALISED in:
 *   - Base case missing initialization (n == 0 path doesn't set result)
 *   - Missing negative path (n < 0 not handled)
 *   - Bounded recursion gap (n >= 10 not handled)
 * - Expected: 3 RETURN_NOT_ALWAYS_INITIALISED errors
 * <br/>
 * 4. type_coercion_initialization.ek9 - Type coercion with uninitialized variables
 * - Pattern: Automatic type coercion (Integer/Float promotion) with uninitialized variables
 * - Error: Flow analysis must detect uninitialized variables even when type coercion occurs
 * - Tests USED_BEFORE_INITIALISED in:
 *   - Integer + Float coercion with uninitialized Float
 *   - Integer - Float coercion with uninitialized Float
 *   - Integer * Float coercion with uninitialized Float
 * - Expected: 3 USED_BEFORE_INITIALISED errors
 * <br/>
 * 5. operator_precedence_initialization.ek9 - Operator precedence with initialization
 * - Pattern: Complex expressions with multiple operators and precedence rules
 * - Error: ALL operands must be checked regardless of evaluation order
 * - Tests USED_BEFORE_INITIALISED in:
 *   - Multiplication before addition (a + b * 2, b uninitialized)
 *   - Left-to-right evaluation (a * b / c, c uninitialized)
 *   - Parentheses override precedence ((a + b) * c, b uninitialized)
 * - Expected: 3 USED_BEFORE_INITIALISED errors
 * <br/>
 * 6. exception_throwing_initialization.ek9 - Exception throwing with uninitialized variables
 * - Pattern: Exception construction/throwing with uninitialized values
 * - Error: Exception paths must respect initialization rules
 * - Tests USED_BEFORE_INITIALISED in:
 *   - Throw with uninitialized message string
 *   - Throw with uninitialized value in string interpolation
 *   - Conditional throw with uninitialized value in one path
 * - Expected: 3 USED_BEFORE_INITIALISED errors
 * </p>
 * <p>Flow Analysis Semantics:
 * - USED_BEFORE_INITIALISED: Variables with '?' marker must be initialized before use
 * - RETURN_NOT_ALWAYS_INITIALISED: Return values with '?' must be initialized on ALL paths
 * - Expression contexts: All expression types (arithmetic, logical, comparison, ternary)
 * - String interpolation: ${variable} embedded expressions count as usage
 * - Recursive functions: Flow analysis tracks initialization across recursive boundaries
 * - Errors detected at PRE_IR_CHECKS phase during flow analysis
 * </p>
 * <p>Expected behavior:
 * - Compiler should detect uninitialized variables in all expression contexts
 * - Compiler should detect uninitialized variables in string interpolation
 * - Compiler should detect missing return initialization in recursive functions
 * - Flow analysis should track initialization across all control flow paths
 * </p>
 * <p>Validates: Flow analysis correctly prevents use of uninitialized variables
 * in expression contexts, string interpolation, and recursive functions, ensuring type safety.
 * </p>
 * <p>Gap addressed: Six critical gaps identified:
 * - Expression context initialization: Existing 53 USED_BEFORE_INITIALISED tests
 *   focused on statement contexts; expression contexts (arithmetic, logical, comparison)
 *   had minimal coverage. This test adds 5 targeted expression context scenarios.
 * - Text interpolation: ZERO existing tests for string interpolation with
 *   uninitialized variables. This test adds 4 interpolation scenarios covering
 *   simple, multi-variable, numeric, and complex patterns.
 * - Recursive function initialization: 40 existing RETURN_NOT_ALWAYS_INITIALISED tests
 *   but limited recursive function coverage. This test adds 3 recursive-specific scenarios
 *   covering base case gaps, missing paths, and bounded recursion.
 * - Type coercion initialization: No existing tests for automatic type promotion
 *   (Integer/Float) with uninitialized variables. This test adds 3 coercion scenarios.
 * - Operator precedence initialization: No existing tests verifying that operator
 *   precedence doesn't bypass initialization checking. This test adds 3 precedence scenarios.
 * - Exception throwing initialization: No existing tests for throwing exceptions with
 *   uninitialized values. This test adds 3 exception path scenarios.
 * These tests ensure EK9's flow analysis is comprehensive across ALL usage contexts.
 * Total: 21 errors across 6 test files
 * </p>
 */
class FlowAnalysisFuzzTest extends FuzzTestBase {

  public FlowAnalysisFuzzTest() {
    super("flowAnalysis", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testFlowAnalysisRobustness() {
    assertTrue(runTests() != 0);
  }
}
