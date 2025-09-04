package org.ek9lang.compiler.ir;

import java.util.Set;

/**
 * Metadata about method/function/operator calls that can be used by backends for optimization.
 * Includes purity information, complexity scoring, and side effect classification.
 */
public record CallMetaData(boolean isPure,
                           int complexityScore,
                           Set<String> sideEffects) {

  /**
   * Creates CallMetaData with no side effects.
   */
  public CallMetaData(boolean isPure, int complexityScore) {
    this(isPure, complexityScore, Set.of());
  }

  /**
   * Creates default CallMetaData for when no symbol information is available.
   * Assumes non-pure with zero complexity and no known side effects.
   */
  public static CallMetaData defaultMetaData() {
    return new CallMetaData(false, 0, Set.of());
  }
}