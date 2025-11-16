package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for cyclomatic complexity edge cases (PRE_IR_CHECKS phase).
 * Tests EXCESSIVE_COMPLEXITY error detection for deeply nested expressions.
 *
 * <p>Test corpus: fuzzCorpus/complexityEdgeCases (1 test file)
 * Validates that deeply nested boolean expressions correctly contribute to
 * cyclomatic complexity calculations and trigger threshold violations.
 *
 * <p>Test scenarios:
 * 1. deep_boolean_expression_complexity.ek9 - Deep boolean expression nesting
 * - Pattern: Boolean and/or operators in deeply nested structure
 * - Each Boolean and/or adds +1 to complexity (short-circuit branching)
 * - Target: Exceed 50 complexity threshold (achieves 58, 8 over limit)
 * - Expected: 1 EXCESSIVE_COMPLEXITY error
 *
 * <p>Why This Is a Genuine Edge Case:
 * Existing tests (badComplexityExamples.ek9) cover basic complexity violations
 * with if/else, loops, and simple boolean logic. This test pushes the boundary
 * with extreme boolean expression nesting (57 and/or operators), validating
 * that the complexity calculator correctly handles deeply nested AST structures.
 *
 * <p>Complexity Calculation Rules:
 * - Boolean and/or: +1 (short-circuit branching semantics)
 * - Bits and/or: +0 (no branching, bitwise operations)
 * - Each if/else/while/for: +1 per decision point
 * - Function base: +1
 * - Threshold: 50 for functions/operators, 500 for classes
 *
 * <p>Expected behavior:
 * - Deep boolean expressions accumulate complexity correctly
 * - Compiler detects threshold violations (50 for functions)
 * - Error message clearly identifies the excessive complexity
 *
 * <p>Validates: EK9's cyclomatic complexity enforcement maintains code quality
 * by preventing overly complex logic that would be difficult to test and maintain.
 *
 * <p>Total: 1 EXCESSIVE_COMPLEXITY error across 1 test file
 */
class ComplexityEdgeCasesFuzzTest extends FuzzTestBase {

  public ComplexityEdgeCasesFuzzTest() {
    super("complexityEdgeCases", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testComplexityEdgeCasesRobustness() {
    assertTrue(runTests() != 0);
  }
}
