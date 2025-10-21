package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Specialized IR instruction for control flow operations (BRANCH, BRANCH_TRUE, BRANCH_FALSE, ASSERT, RETURN).
 * <p>
 * Branch instructions handle all control flow in the EK9 IR, including loops,
 * conditionals, and method returns.
 * </p>
 */
public final class BranchInstr extends IRInstr {

  /**
   * Create return with no value: RETURN.
   */
  public static BranchInstr returnVoid() {
    return new BranchInstr(IROpcode.RETURN, null, null);
  }

  /**
   * Create return with no value and debug info: RETURN.
   */
  public static BranchInstr returnVoid(final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.RETURN, null, debugInfo);
  }

  /**
   * Create return with value and debug info: RETURN value.
   */
  public static BranchInstr returnValue(final String value, final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.RETURN, null, debugInfo).addOperand(value);
  }

  /**
   * Create assert instruction with debug info: ASSERT condition.
   */
  public static BranchInstr assertValue(final String condition, final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.ASSERT, null, debugInfo).addOperand(condition);
  }

  private BranchInstr(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public BranchInstr addOperand(final String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public BranchInstr addOperands(final String... operands) {
    super.addOperands(operands);
    return this;
  }

  /**
   * Get target label for branch instructions.
   */
  public java.lang.String getTargetLabel() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.BRANCH) {
      return operands.isEmpty() ? null : operands.getFirst();
    } else if (getOpcode() == IROpcode.BRANCH_TRUE || getOpcode() == IROpcode.BRANCH_FALSE) {
      return operands.size() < 2 ? null : operands.getLast();
    }
    return null;
  }

  /**
   * Get condition for conditional branch instructions.
   */
  public java.lang.String getCondition() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.BRANCH_TRUE || getOpcode() == IROpcode.BRANCH_FALSE) {
      return operands.isEmpty() ? null : operands.getFirst();
    }
    return null;
  }

  /**
   * Get return value for RETURN instructions.
   */
  public java.lang.String getReturnValue() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.RETURN) {
      return operands.isEmpty() ? null : operands.getFirst();
    }
    return null;
  }

  /**
   * Get condition for ASSERT instructions.
   */
  public java.lang.String getAssertCondition() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.ASSERT) {
      return operands.isEmpty() ? null : operands.getFirst();
    }
    return null;
  }
}