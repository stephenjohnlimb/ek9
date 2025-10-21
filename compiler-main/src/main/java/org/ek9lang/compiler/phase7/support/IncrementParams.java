package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Parameters for increment/decrement evaluation.
 * <p>
 * Encapsulates all data needed to perform an increment or decrement
 * operation and update the loop counter variable.
 * </p>
 *
 * @param counterVar Variable name holding the current counter value
 * @param operator Increment operator ("++") or decrement operator ("--")
 * @param counterType Type of the counter variable
 * @param incrementResultTemp Temporary variable for increment/decrement result
 * @param scopeId Scope ID for memory management
 * @param debugInfo Debug information for instructions
 */
public record IncrementParams(
    String counterVar,
    String operator,
    ISymbol counterType,
    String incrementResultTemp,
    String scopeId,
    DebugInfo debugInfo
) {
}
