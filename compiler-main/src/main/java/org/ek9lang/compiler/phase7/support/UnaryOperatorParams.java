package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Parameters for unary operator invocation.
 * <p>
 * Encapsulates all data needed to invoke a unary operator with proper
 * memory management and type resolution.
 * </p>
 *
 * @param operand Variable name holding the operand
 * @param operator Operator method name (e.g., "++", "--", "_neg")
 * @param operandType Type of the operand
 * @param resultType Expected result type
 * @param resultTemp Variable name to store result
 * @param scopeId Scope ID for memory management
 * @param debugInfo Debug information for instructions
 */
public record UnaryOperatorParams(
    String operand,
    String operator,
    ISymbol operandType,
    ISymbol resultType,
    String resultTemp,
    String scopeId,
    DebugInfo debugInfo
) {
}
