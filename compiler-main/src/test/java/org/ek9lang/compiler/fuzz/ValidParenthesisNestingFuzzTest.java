package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for parenthesis nesting in expressions.
 * Tests compiler robustness with deeply nested and wide parenthesis structures.
 *
 * <p>Parentheses don't add to cyclomatic complexity (no execution branches),
 * but they create deep AST structures that stress:
 * <ul>
 *   <li>Parser recursion handling</li>
 *   <li>AST tree traversal algorithms</li>
 *   <li>Type inference propagation</li>
 *   <li>IR generation (expression linearization)</li>
 *   <li>Code generation (operand stack management)</li>
 * </ul>
 *
 * <p>Test corpus: fuzzCorpus/complexExpressions/validParenthesisNesting (5 test files)
 * Validates:
 * <ul>
 *   <li>deepArithmeticNesting.ek9 - 10, 20, 30 levels of nesting with operations</li>
 *   <li>wideParenthesisExpressions.ek9 - 10, 15, 20 groups at same level</li>
 *   <li>mixedDepthWidthPatterns.ek9 - Pyramid, tree, and formula patterns</li>
 *   <li>parenthesisWithMethodForm.ek9 - Method-form operators with grouping</li>
 *   <li>booleanParenthesisNesting.ek9 - Boolean logic with nested parentheses</li>
 * </ul>
 *
 * <p>Key Finding: Cannot chain method calls on parenthesized expressions.
 * Pattern (a.+(b)).*(c) does NOT work - must use intermediate variables.
 *
 * <p>Design Consideration: Should parentheses add to complexity?
 * <ul>
 *   <li>Current: No (no execution branches)</li>
 *   <li>Alternative: Depth-based limit (like nesting depth for control flow)</li>
 *   <li>Rationale: Cognitive complexity vs cyclomatic complexity distinction</li>
 * </ul>
 *
 * <p>Expected behavior: All valid parenthesis nesting tests compile successfully.
 *
 * @see ValidDualFormOperatorFuzzTest for operator form tests
 * @see ValidArithmeticExpressionFuzzTest for arithmetic expression tests
 */
class ValidParenthesisNestingFuzzTest extends FuzzTestBase {

  private static final int EXPECTED_FILE_COUNT = 5;

  public ValidParenthesisNestingFuzzTest() {
    super("complexExpressions/validParenthesisNesting", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testParenthesisNestingRobustness() {
    assertEquals(EXPECTED_FILE_COUNT, runTests(),
        "Expected " + EXPECTED_FILE_COUNT + " valid parenthesis nesting test files");
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                    final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult, "Valid parenthesis nesting tests should compile successfully");
    assertEquals(0, numberOfErrors, "Valid parenthesis nesting tests should have zero errors");
  }
}
