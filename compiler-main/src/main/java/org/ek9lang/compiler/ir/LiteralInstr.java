package org.ek9lang.compiler.ir;

import org.ek9lang.core.AssertValue;

/**
 * IR instruction for loading literal/constant values.
 * Handles string literals, numeric literals, boolean literals, etc.
 * Uses symbol-driven approach to get correct type information.
 */
public final class LiteralInstr extends IRInstr {

  private LiteralInstr(final String result,
                       final String literalValue,
                       final String literalType,
                       final DebugInfo debugInfo) {

    super(IROpcode.LOAD_LITERAL, result, debugInfo);

    AssertValue.checkNotNull("literalValue cannot be null", literalValue);
    AssertValue.checkNotNull("literalType cannot be null", literalType);

    // Store the literal value and its type using the base class method
    addOperand(literalValue);
    addOperand(literalType);
  }

  /**
   * Create instruction to load any literal value.
   *
   * @param result       Variable to store the literal value
   * @param literalValue The literal value as string
   * @param literalType  The resolved type name (could be decorated)
   * @param debugInfo    Debug information for source mapping (can be null)
   * @return LiteralInstruction for loading the literal
   */
  public static LiteralInstr literal(final String result,
                                     final String literalValue,
                                     final String literalType,
                                     final DebugInfo debugInfo) {
    return new LiteralInstr(result, literalValue, literalType, debugInfo);
  }


  public static LiteralInstr literal(final String result,
                                     final String literalValue,
                                     final String literalType) {
    return literal(result, literalValue, literalType, null);
  }

  /**
   * Get the literal value.
   */
  public String getLiteralValue() {
    return getOperands().getFirst();
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