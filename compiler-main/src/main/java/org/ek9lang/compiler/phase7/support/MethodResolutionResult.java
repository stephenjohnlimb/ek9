package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Result of method resolution including cost analysis.
 * Contains the resolved method and information about whether promotion is required.
 */
public record MethodResolutionResult(
    MethodSymbol methodSymbol,
    double matchPercentage,
    boolean requiresPromotion
) {

  /**
   * Check if this is a perfect match (no coercion needed).
   */
  public boolean isPerfectMatch() {
    return Math.abs(matchPercentage - 100.0) < 0.001;
  }

  /**
   * Check if this method resolution failed.
   */
  public boolean isInvalid() {
    return matchPercentage < 0.0;
  }

  /**
   * Get the method's return type name.
   */
  public String getReturnTypeName() {
    return methodSymbol.getType().map(ISymbol::getFullyQualifiedName).orElse("Void");
  }
}