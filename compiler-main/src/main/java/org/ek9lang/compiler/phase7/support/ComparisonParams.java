package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Parameters for comparison evaluation.
 * <p>
 * Encapsulates all data needed to perform a comparison operation
 * and extract the primitive boolean result.
 * </p>
 *
 * @param leftOperand Variable name holding the left operand
 * @param rightOperand Variable name holding the right operand
 * @param operator Comparison operator (e.g., "&lt;", "&lt;=", "&gt;", "&gt;=", "==")
 * @param leftType Type of left operand
 * @param rightType Type of right operand
 * @param booleanType EK9 Boolean type symbol
 * @param booleanObjectTemp Temporary variable for EK9 Boolean result
 * @param primitiveBooleanTemp Temporary variable for primitive boolean result
 * @param scopeId Scope ID for memory management
 * @param debugInfo Debug information for instructions
 */
public record ComparisonParams(
    String leftOperand,
    String rightOperand,
    String operator,
    ISymbol leftType,
    ISymbol rightType,
    ISymbol booleanType,
    String booleanObjectTemp,
    String primitiveBooleanTemp,
    String scopeId,
    DebugInfo debugInfo
) {
}
