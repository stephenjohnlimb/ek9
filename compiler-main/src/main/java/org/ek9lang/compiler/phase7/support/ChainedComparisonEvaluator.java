package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * CONCERN: Chained comparison evaluation for polymorphic for-range.
 * RESPONSIBILITY: Evaluate direction check, then condition check based on result.
 * REUSABILITY: FOR_RANGE_POLYMORPHIC direction detection.
 * <p>
 * Composite helper that chains multiple ComparisonEvaluator calls
 * to implement runtime direction detection in polymorphic for-range loops.
 * </p>
 * <p>
 * Pattern:
 * 1. Evaluate direction comparison (e.g., "range._cmp(0) &lt; 0")
 * 2. Branch based on direction result
 * 3. Evaluate appropriate condition (ascending: "&lt;=", descending: "&gt;=")
 * </p>
 * <p>
 * This eliminates the duplication between generateDirectionLessThanZero
 * and generateDirectionGreaterThanZero in ForStatementGenerator.
 * </p>
 */
public final class ChainedComparisonEvaluator implements Function<ChainedComparisonParams, List<IRInstr>> {
  private final ComparisonEvaluator comparisonEvaluator;

  public ChainedComparisonEvaluator(final ComparisonEvaluator comparisonEvaluator) {
    this.comparisonEvaluator = comparisonEvaluator;
  }

  /**
   * Evaluate chained comparison operations.
   * <p>
   * Handles:
   * 1. First comparison (e.g., direction check: "range._cmp(0) &lt; 0")
   * 2. Second comparison (e.g., loop condition: "counter &lt;= end")
   * 3. Memory management for all intermediate values
   * </p>
   *
   * @param params Chained comparison parameters
   * @return Instructions ending with final primitive boolean result
   */
  @Override
  public List<IRInstr> apply(final ChainedComparisonParams params) {
    final var instructions = new ArrayList<IRInstr>();

    // Step 1: Evaluate first comparison (e.g., direction check)
    instructions.addAll(comparisonEvaluator.apply(params.firstComparison()));

    // Step 2: Evaluate second comparison (e.g., loop condition)
    instructions.addAll(comparisonEvaluator.apply(params.secondComparison()));

    return instructions;
  }
}
