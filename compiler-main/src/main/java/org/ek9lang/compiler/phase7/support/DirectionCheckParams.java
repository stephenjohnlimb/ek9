package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Parameters for direction check evaluation.
 * <p>
 * Encapsulates all data needed to build direction check IR for
 * polymorphic for-range loops (direction &lt; 0 or direction &gt; 0).
 * </p>
 *
 * @param directionTemp Variable holding the direction value (from start &lt;=&gt; end)
 * @param comparisonOperator Comparison operator ("&lt;" for ascending, "&gt;" for descending)
 * @param zeroTemp Temporary variable for literal 0
 * @param booleanObjectTemp Temporary variable for EK9 Boolean comparison result
 * @param primitiveBooleanTemp Temporary variable for primitive boolean result
 * @param integerType EK9 Integer type symbol
 * @param booleanType EK9 Boolean type symbol
 * @param scopeId Scope ID for memory management
 * @param debugInfo Debug information for instructions
 */
public record DirectionCheckParams(
    String directionTemp,
    String comparisonOperator,
    String zeroTemp,
    String booleanObjectTemp,
    String primitiveBooleanTemp,
    ISymbol integerType,
    ISymbol booleanType,
    String scopeId,
    DebugInfo debugInfo
) {
}
