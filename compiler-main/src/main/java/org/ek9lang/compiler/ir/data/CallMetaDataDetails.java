package org.ek9lang.compiler.ir.data;

import java.util.Set;

/**
 * Metadata about method/function/operator calls that can be used by backends for optimization.
 * Includes purity information, complexity scoring, and side effect classification.
 */
public record CallMetaDataDetails(boolean isPure,
                                  int complexityScore,
                                  Set<String> sideEffects) {

  /**
   * Creates CallMetaDataDetails with no side effects.
   */
  public CallMetaDataDetails(boolean isPure, int complexityScore) {
    this(isPure, complexityScore, Set.of());
  }

  /**
   * Creates default CallMetaDataDetails for when no symbol information is available.
   * Assumes non-pure with zero complexity and no known side effects.
   */
  public static CallMetaDataDetails defaultMetaData() {
    return new CallMetaDataDetails(false, 0, Set.of());
  }
}