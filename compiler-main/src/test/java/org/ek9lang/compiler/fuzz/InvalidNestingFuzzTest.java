package org.ek9lang.compiler.fuzz;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.ek9lang.compiler.CompilationPhase;
import org.junit.jupiter.api.Test;

/**
 * Fuzzing tests for excessive nesting triggering EXCESSIVE_COMPLEXITY and EXCESSIVE_NESTING errors.
 * Tests that deep control flow nesting correctly triggers appropriate error limits.
 *
 * <p>Design Constraints: EK9 enforces two separate limits:
 * <ul>
 *   <li>Cyclomatic Complexity: Functions/Methods/Operators max = 50, Types/Classes max = 500</li>
 *   <li>Nesting Depth: Maximum 10 levels for all control flow structures</li>
 * </ul>
 *
 * <p>Test corpus: fuzzCorpus/mutations/invalid/nesting (4 test files)
 * Validates that excessive nesting correctly triggers appropriate errors:
 *
 * <p>EXCESSIVE_COMPLEXITY test scenarios:
 * <ol>
 *   <li>nesting_if_060.ek9 - 60 nested if statements
 *       <ul>
 *         <li>Complexity: 1 (base) + 60 (if blocks) = 61</li>
 *         <li>Expected: EXCESSIVE_COMPLEXITY (complexity detected first in check chain)</li>
 *       </ul>
 *   </li>
 *   <li>nesting_if_100.ek9 - 100 nested if statements
 *       <ul>
 *         <li>Complexity: 1 (base) + 100 (if blocks) = 101</li>
 *         <li>Also stress tests parser with very deep AST</li>
 *         <li>Expected: EXCESSIVE_COMPLEXITY (complexity detected first in check chain)</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>EXCESSIVE_NESTING test scenarios:
 * <ol>
 *   <li>nesting_depth_011.ek9 - 11 nested if statements (pure if nesting)
 *       <ul>
 *         <li>Nesting depth: 11 (exceeds max 10 by 1)</li>
 *         <li>Complexity: 23 (well under 50 limit)</li>
 *         <li>Expected: EXCESSIVE_NESTING (complexity OK, nesting exceeded)</li>
 *       </ul>
 *   </li>
 *   <li>nesting_mixed_011.ek9 - 11 levels with mixed control flow types
 *       <ul>
 *         <li>Pattern: if → while → for → switch → try → if → while → for → switch → try → if</li>
 *         <li>Proves nesting tracking is agnostic to control flow type</li>
 *         <li>Complexity: ~25 (well under 50 limit)</li>
 *         <li>Expected: EXCESSIVE_NESTING (complexity OK, nesting exceeded)</li>
 *       </ul>
 *   </li>
 * </ol>
 *
 * <p>Expected behavior:
 * <ul>
 *   <li>Compiler detects functions exceeding 50 complexity threshold (EXCESSIVE_COMPLEXITY)</li>
 *   <li>Compiler detects control flow exceeding 10 nesting depth (EXCESSIVE_NESTING)</li>
 *   <li>Deep nesting is counted correctly toward both metrics</li>
 *   <li>Parser handles extreme nesting without crash (graceful error)</li>
 *   <li>Nesting depth tracking is agnostic to control flow type (if/while/for/switch/try)</li>
 * </ul>
 *
 * <p>Validates: Both cyclomatic complexity and nesting depth limits are correctly
 * enforced on deeply nested control flow structures.
 *
 * @see ValidNestingMutationTest for tests within both complexity and nesting limits
 */
class InvalidNestingFuzzTest extends FuzzTestBase {

  public InvalidNestingFuzzTest() {
    super("mutations/invalid/nesting", CompilationPhase.PRE_IR_CHECKS);
  }

  @Test
  void testExcessiveNestingComplexity() {
    assertTrue(runTests() != 0, "Expected invalid nesting tests to be processed");
  }
}
