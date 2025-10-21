package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Parameters for literal value loading.
 * <p>
 * Encapsulates all data needed to load a literal value with proper
 * memory management and type resolution.
 * </p>
 *
 * @param tempName Variable name to store the literal
 * @param literalValue The literal value to load
 * @param literalType Type of the literal
 * @param debugInfo Debug information for instructions
 */
public record LiteralParams(
    String tempName,
    String literalValue,
    ISymbol literalType,
    DebugInfo debugInfo
) {
}
