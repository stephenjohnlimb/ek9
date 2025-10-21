package org.ek9lang.compiler.phase7.support;

/**
 * Parameters for chained comparison evaluation.
 * <p>
 * Encapsulates two comparison operations that are evaluated sequentially,
 * typically used for direction detection in polymorphic for-range loops.
 * </p>
 *
 * @param firstComparison First comparison to evaluate (e.g., direction check)
 * @param secondComparison Second comparison to evaluate (e.g., loop condition)
 */
public record ChainedComparisonParams(
    ComparisonParams firstComparison,
    ComparisonParams secondComparison
) {
}
