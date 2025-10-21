package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Parameters for binary operator invocation.
 * <p>
 * Encapsulates all data needed to invoke a binary operator with proper
 * memory management and type resolution.
 * </p>
 *
 * @param leftOperand Variable name holding the left operand
 * @param rightOperand Variable name holding the right operand
 * @param operator Operator method name (e.g., "+", "&lt;", "&lt;=&gt;")
 * @param leftType Type of left operand
 * @param rightType Type of right operand
 * @param resultType Expected result type
 * @param resultTemp Variable name to store result
 * @param scopeId Scope ID for memory management
 * @param debugInfo Debug information for instructions
 */
public record BinaryOperatorParams(
    String leftOperand,
    String rightOperand,
    String operator,
    ISymbol leftType,
    ISymbol rightType,
    ISymbol resultType,
    String resultTemp,
    String scopeId,
    DebugInfo debugInfo
) {
}
