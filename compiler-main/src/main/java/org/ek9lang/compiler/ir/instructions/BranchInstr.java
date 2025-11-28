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
   * Create assert instruction with message and debug info: ASSERT condition, message.
   * Message is stored as second operand and available at runtime regardless of debug mode.
   */
  public static BranchInstr assertValue(final String condition, final String message, final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.ASSERT, null, debugInfo)
        .addOperand(condition)
        .addOperand(message != null ? message : "");
  }

  /**
   * Create assert instruction with debug info: ASSERT condition.
   * Backward compatible - no message.
   */
  public static BranchInstr assertValue(final String condition, final DebugInfo debugInfo) {
    return assertValue(condition, "", debugInfo);
  }

  /**
   * Create unconditional branch: BRANCH target_label.
   */
  public static BranchInstr branch(final String targetLabel, final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.BRANCH, null, debugInfo).addOperand(targetLabel);
  }

  /**
   * Create conditional branch if true: BRANCH_TRUE condition, target_label.
   * Branches to target if condition is true (non-zero).
   */
  public static BranchInstr branchIfTrue(final String condition,
                                         final String targetLabel,
                                         final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.BRANCH_TRUE, null, debugInfo)
        .addOperand(condition)
        .addOperand(targetLabel);
  }

  /**
   * Create conditional branch if false: BRANCH_FALSE condition, target_label.
   * Branches to target if condition is false (zero).
   */
  public static BranchInstr branchIfFalse(final String condition,
                                          final String targetLabel,
                                          final DebugInfo debugInfo) {
    return new BranchInstr(IROpcode.BRANCH_FALSE, null, debugInfo)
        .addOperand(condition)
        .addOperand(targetLabel);
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

  /**
   * Get assertion message (second operand).
   * Returns null if no message was provided.
   */
  public java.lang.String getAssertMessage() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.ASSERT && operands.size() > 1) {
      java.lang.String msg = operands.get(1);
      return msg.isEmpty() ? null : msg;
    }
    return null;
  }
}