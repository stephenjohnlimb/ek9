package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for expressions that exceed complexity limits.
 * Tests EXCESSIVE_COMPLEXITY error detection in complex expressions.
 *
 * <p>EK9 Complexity Limits (from AcceptableConstructComplexityOrError.java):
 * <ul>
 *   <li>Functions/Methods/Operators: Maximum complexity = 50</li>
 *   <li>Types/Classes: Maximum complexity = 500</li>
 * </ul>
 *
 * <p>Complexity Contributors (from PreIRListener.java):
 * <ul>
 *   <li>Comparison operators (&lt;, &gt;, ==, etc.): +1 each</li>
 *   <li>Boolean logic operators (and, or): +1 each (short-circuit branching)</li>
 *   <li>if control blocks: +1 each</li>
 *   <li>case expressions (switch): +1 per case</li>
 *   <li>for loops: +1 (or +2 with BY clause)</li>
 *   <li>while loops: +1</li>
 *   <li>try/catch/finally: +1 each</li>
 *   <li>throw statements: +2</li>
 * </ul>
 *
 * <p>Test corpus: fuzzCorpus/complexExpressions/invalidComplexity (2 test files)
 * Validates:
 * <ul>
 *   <li>excessiveIfComplexity.ek9 - 26 sequential if blocks + 26 comparisons = 54 complexity</li>
 *   <li>excessiveLoopComplexity.ek9 - 17 for-range loops + if blocks = 53 complexity</li>
 * </ul>
 *
 * <p>Note: Boolean complexity is covered by complexityEdgeCases/deep_boolean_expression_complexity.ek9
 * and nested if complexity by mutations/invalid/nesting/nesting_if_*.ek9
 *
 * <p>Expected behavior:
 * <ul>
 *   <li>Compiler detects functions exceeding 50 complexity threshold</li>
 *   <li>EXCESSIVE_COMPLEXITY (E11010) error is reported</li>
 *   <li>Error message includes calculated complexity value</li>
 * </ul>
 *
 * <p>Validates: Complexity analysis correctly detects and reports
 * EXCESSIVE_COMPLEXITY for complex boolean, control flow, and loop patterns.
 *
 * @see ValidDualFormOperatorFuzzTest for valid expression tests
 * @see ValidArithmeticExpressionFuzzTest for valid arithmetic tests
 */
class InvalidComplexityExpressionFuzzTest extends FuzzTestBase {

  public InvalidComplexityExpressionFuzzTest() {
    super("complexExpressions/invalidComplexity", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testExcessiveComplexityDetection() {
    assertTrue(runTests() != 0, "Expected invalid complexity expression tests to be processed");
  }
}
