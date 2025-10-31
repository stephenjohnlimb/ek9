package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for expression syntax robustness.
 * Tests parser-level handling of malformed operators, brackets, and expression structure.
 *
 * <p>Each test in fuzzCorpus/expressionSyntax contains malformed expression syntax:
 * - Missing operands (left or right)
 * - Consecutive operators (a + * b)
 * - Unbalanced brackets/parentheses
 * - Invalid operators (++, --, **)
 * - Incomplete ternary expressions
 * - Malformed list literals
 * - Trailing operators
 *
 * <p>We expect these to fail at PARSING phase but NOT crash the compiler.
 */
class ExpressionSyntaxFuzzTest extends FuzzTestBase {

  public ExpressionSyntaxFuzzTest() {
    super("expressionSyntax", CompilationPhase.PARSING);
  }

  @Test
  void testExpressionSyntaxRobustness() {
    assertTrue(runTests() != 0);
  }
}
