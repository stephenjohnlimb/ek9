package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilableProgram;
import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzz tests for dual-form operator syntax.
 * Tests that operators can be used in both operator form and method form.
 *
 * <p>EK9 operators can be used in two equivalent forms:
 * <ul>
 *   <li>Operator form: {@code a + b}, {@code sqrt 16}, {@code a < b}</li>
 *   <li>Method form: {@code a.+(b)}, {@code value.sqrt()}, {@code a.<(b)}</li>
 * </ul>
 *
 * <p>Test corpus: fuzzCorpus/complexExpressions/validDualForm (5 test files)
 * Validates:
 * <ul>
 *   <li>dualFormUnaryOperators.ek9 - sqrt, abs in both forms</li>
 *   <li>dualFormBinaryOperators.ek9 - +, -, *, /, mod, ^ in both forms</li>
 *   <li>dualFormComparisonOperators.ek9 - &lt;, &lt;=, &gt;, &gt;=, ==, &lt;&gt;, &lt;=&gt; in both forms</li>
 *   <li>dualFormBooleanOperators.ek9 - and, or, xor in both forms (not is prefix-only)</li>
 *   <li>dualFormMixedChaining.ek9 - mixed operator/method forms and chaining</li>
 * </ul>
 *
 * <p>Key Findings:
 * <ul>
 *   <li>Literals cannot be objectAccessStart - must use variables for method form</li>
 *   <li>The 'not' operator has no method form - it's prefix-only</li>
 *   <li>Method chaining works: a.+(3).*(2) evaluates left to right</li>
 *   <li>Mixed forms work: a + b.*(2) respects precedence</li>
 * </ul>
 *
 * <p>Expected behavior: All valid dual-form tests compile successfully.
 *
 * @see InvalidComplexityExpressionFuzzTest for tests that exceed complexity limits
 */
class ValidDualFormOperatorFuzzTest extends FuzzTestBase {

  private static final int EXPECTED_FILE_COUNT = 5;

  public ValidDualFormOperatorFuzzTest() {
    super("complexExpressions/validDualForm", CompilationPhase.PRE_IR_CHECKS, false);
  }

  @Test
  void testDualFormOperatorRobustness() {
    assertEquals(EXPECTED_FILE_COUNT, runTests(),
        "Expected " + EXPECTED_FILE_COUNT + " valid dual-form operator test files");
  }

  @Override
  protected void assertFinalResults(final boolean compilationResult,
                                    final int numberOfErrors,
                                    final CompilableProgram program) {
    assertTrue(compilationResult, "Valid dual-form operator tests should compile successfully");
    assertEquals(0, numberOfErrors, "Valid dual-form operator tests should have zero errors");
  }
}
