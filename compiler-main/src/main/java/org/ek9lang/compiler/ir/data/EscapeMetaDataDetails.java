package org.ek9lang.compiler.ir.data;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Escape analysis metadata for IR optimisation phase.
 * Indicates whether values can escape their current scope and enables
 * backend optimizations like stack allocation instead of reference counting.
 */
public record EscapeMetaDataDetails(EscapeLevel escapeLevel,
                                    LifetimeScope lifetimeScope,
                                    Set<String> optimizationHints) {

  /**
   * Creates metadata for values that don't escape their scope.
   */
  public static EscapeMetaDataDetails noEscape(LifetimeScope lifetimeScope) {
    return new EscapeMetaDataDetails(EscapeLevel.NONE, lifetimeScope, Set.of("STACK_CANDIDATE"));
  }

  /**
   * Creates metadata for values that escape to parameters.
   */
  public static EscapeMetaDataDetails escapeParameter(LifetimeScope lifetimeScope) {
    return new EscapeMetaDataDetails(EscapeLevel.PARAMETER, lifetimeScope, Set.of());
  }

  /**
   * Creates metadata for values that escape globally.
   */
  public static EscapeMetaDataDetails escapeGlobal(LifetimeScope lifetimeScope) {
    return new EscapeMetaDataDetails(EscapeLevel.GLOBAL, lifetimeScope, Set.of());
  }

  /**
   * Escape level classification.
   */
  public enum EscapeLevel {
    NONE,       // Value doesn't escape current scope
    LOCAL,      // Value escapes to local variables but not parameters/fields
    PARAMETER,  // Value escapes via parameter passing
    FIELD,      // Value escapes via field assignment
    RETURN,     // Value escapes via return statement
    GLOBAL      // Value escapes to global/static scope
  }

  /**
   * Lifetime scope classification.
   */
  public enum LifetimeScope {
    LOCAL_SCOPE,    // Lives only within current basic block/scope
    FUNCTION,       // Lives for entire function duration
    MODULE,         // Lives for module duration
    STATIC,         // Lives for entire program duration
    UNKNOWN         // Lifetime cannot be determined
  }

  @Override
  @Nonnull
  public String toString() {
    final var sb = new StringBuilder();
    sb.append("[escape=").append(escapeLevel);
    
    if (lifetimeScope != LifetimeScope.UNKNOWN) {
      sb.append(", lifetime=").append(lifetimeScope);
    }
    
    if (!optimizationHints.isEmpty()) {
      sb.append(", hints=").append(String.join(",", optimizationHints));
    }
    
    sb.append("]");
    return sb.toString();
  }
}