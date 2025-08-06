package org.ek9lang.compiler.ir;

import org.ek9lang.core.AssertValue;

/**
 * IR instruction for loading literal/constant values.
 * Handles string literals, numeric literals, boolean literals, etc.
 * Uses symbol-driven approach to get correct type information.
 */
public final class LiteralInstruction extends IRInstruction {

  private LiteralInstruction(final String result, final String literalValue, final String literalType, final DebugInfo debugInfo) {
    super(IROpcode.LOAD_LITERAL, result, debugInfo);
    AssertValue.checkNotNull("literalValue cannot be null", literalValue);
    AssertValue.checkNotNull("literalType cannot be null", literalType);
    
    // Store the literal value and its type using the base class method
    addOperand(literalValue);
    addOperand(literalType);
  }

  /**
   * Create instruction to load a string literal.
   * 
   * @param result Variable to store the literal value
   * @param literalValue The actual string value (including quotes)
   * @param literalType The resolved type name (could be decorated for generic contexts)
   * @param debugInfo Debug information for source mapping (can be null)
   * @return LiteralInstruction for loading string literal
   */
  public static LiteralInstruction stringLiteral(final String result, final String literalValue, final String literalType, final DebugInfo debugInfo) {
    return new LiteralInstruction(result, literalValue, literalType, debugInfo);
  }

  /**
   * Create instruction to load a numeric literal.
   * 
   * @param result Variable to store the literal value
   * @param literalValue The numeric value as string
   * @param literalType The resolved type name (Integer, Float, etc.)
   * @param debugInfo Debug information for source mapping (can be null)
   * @return LiteralInstruction for loading numeric literal
   */
  public static LiteralInstruction numericLiteral(final String result, final String literalValue, final String literalType, final DebugInfo debugInfo) {
    return new LiteralInstruction(result, literalValue, literalType, debugInfo);
  }

  /**
   * Create instruction to load a boolean literal.
   * 
   * @param result Variable to store the literal value
   * @param literalValue The boolean value as string ("true" or "false")
   * @param literalType The resolved type name (typically "Boolean")
   * @param debugInfo Debug information for source mapping (can be null)
   * @return LiteralInstruction for loading boolean literal
   */
  public static LiteralInstruction booleanLiteral(final String result, final String literalValue, final String literalType, final DebugInfo debugInfo) {
    return new LiteralInstruction(result, literalValue, literalType, debugInfo);
  }

  /**
   * Create instruction to load any literal value.
   * 
   * @param result Variable to store the literal value
   * @param literalValue The literal value as string
   * @param literalType The resolved type name (could be decorated)
   * @param debugInfo Debug information for source mapping (can be null)
   * @return LiteralInstruction for loading the literal
   */
  public static LiteralInstruction literal(final String result, final String literalValue, final String literalType, final DebugInfo debugInfo) {
    return new LiteralInstruction(result, literalValue, literalType, debugInfo);
  }
  
  // Convenience methods without debug info (for backward compatibility)
  public static LiteralInstruction stringLiteral(final String result, final String literalValue, final String literalType) {
    return stringLiteral(result, literalValue, literalType, null);
  }

  public static LiteralInstruction numericLiteral(final String result, final String literalValue, final String literalType) {
    return numericLiteral(result, literalValue, literalType, null);
  }

  public static LiteralInstruction booleanLiteral(final String result, final String literalValue, final String literalType) {
    return booleanLiteral(result, literalValue, literalType, null);
  }

  public static LiteralInstruction literal(final String result, final String literalValue, final String literalType) {
    return literal(result, literalValue, literalType, null);
  }

  /**
   * Get the literal value.
   */
  public String getLiteralValue() {
    return getOperands().get(0);
  }

  /**
   * Get the literal type (could be decorated for generic contexts).
   */
  public String getLiteralType() {
    return getOperands().get(1);
  }

  @Override
  public String toString() {
    return getResult() + " = " + getOpcode() + " " + getLiteralValue() + " (" + getLiteralType() + ")";
  }
}