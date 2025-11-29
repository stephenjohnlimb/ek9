package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for valid arithmetic and boolean expressions.
 * Tests complex mathematical expressions within complexity limits.
 *
 * <p>Test corpus: fuzzCorpus/complexExpressions/validArithmetic (3 test files)
 * Validates:
 * <ul>
 *   <li>arithmeticPrecedencePEMDAS.ek9 - PEMDAS/BODMAS operator precedence</li>
 *   <li>scientificFormulas.ek9 - Complex scientific formulas (quadratic, distance, etc.)</li>
 *   <li>complexBooleanExpressions.ek9 - Complex boolean logic patterns</li>
 * </ul>
 *
 * <p>Precedence Testing (PEMDAS/BODMAS):
 * <ul>
 *   <li>Parentheses override default precedence</li>
 *   <li>Exponents (^) before multiplication/division</li>
 *   <li>Multiplication/Division before addition/subtraction</li>
 *   <li>Left-to-right evaluation for same precedence</li>
 * </ul>
 *
 * <p>Scientific Formula Examples:
 * <ul>
 *   <li>Quadratic discriminant: b^2 - 4ac</li>
 *   <li>Distance formula: sqrt((x2-x1)^2 + (y2-y1)^2)</li>
 *   <li>Pythagorean theorem: sqrt(a^2 + b^2)</li>
 *   <li>Temperature conversion: C * 9/5 + 32</li>
 * </ul>
 *
 * <p>Boolean Logic Testing:
 * <ul>
 *   <li>Comparison chains: x &gt; 0 and x &lt; 10</li>
 *   <li>Multiple OR conditions: status == 0 or status == 1</li>
 *   <li>Mixed AND/OR with parentheses</li>
 *   <li>Negation with complex expressions</li>
 * </ul>
 *
 * <p>Expected behavior: All valid arithmetic tests compile successfully.
 *
 * @see InvalidComplexityExpressionFuzzTest for tests that exceed complexity limits
 */
class ValidArithmeticExpressionFuzzTest extends FuzzTestBase {

  private static final int EXPECTED_FILE_COUNT = 3;

  public ValidArithmeticExpressionFuzzTest() {
    super("complexExpressions/validArithmetic", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testArithmeticExpressionRobustness() {
    assertEquals(EXPECTED_FILE_COUNT, runTests(),
        "Expected " + EXPECTED_FILE_COUNT + " valid arithmetic expression test files");
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                    final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult, "Valid arithmetic expression tests should compile successfully");
    assertEquals(0, numberOfErrors, "Valid arithmetic expression tests should have zero errors");
  }
}
