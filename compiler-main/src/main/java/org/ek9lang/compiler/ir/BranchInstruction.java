package org.ek9lang.compiler.ir;

import java.util.List;

/**
 * Specialized IR instruction for control flow operations (BRANCH, BRANCH_TRUE, BRANCH_FALSE, RETURN).
 * <p>
 * Branch instructions handle all control flow in the EK9 IR, including loops,
 * conditionals, and method returns.
 * </p>
 */
public final class BranchInstruction extends IRInstruction {

  /**
   * Create unconditional branch: BRANCH target_label
   */
  public static BranchInstruction branch(final String targetLabel) {
    return new BranchInstruction(IROpcode.BRANCH, null, null).addOperand(targetLabel);
  }

  /**
   * Create unconditional branch with debug info: BRANCH target_label
   */
  public static BranchInstruction branch(final String targetLabel, final DebugInfo debugInfo) {
    return new BranchInstruction(IROpcode.BRANCH, null, debugInfo).addOperand(targetLabel);
  }

  /**
   * Create conditional branch (true): BRANCH_TRUE condition, target_label
   */
  public static BranchInstruction branchTrue(final String condition, final String targetLabel) {
    return new BranchInstruction(IROpcode.BRANCH_TRUE, null, null).addOperands(condition, targetLabel);
  }

  /**
   * Create conditional branch (true) with debug info: BRANCH_TRUE condition, target_label
   */
  public static BranchInstruction branchTrue(final String condition, final String targetLabel, final DebugInfo debugInfo) {
    return new BranchInstruction(IROpcode.BRANCH_TRUE, null, debugInfo).addOperands(condition, targetLabel);
  }

  /**
   * Create conditional branch (false): BRANCH_FALSE condition, target_label
   */
  public static BranchInstruction branchFalse(final String condition, final String targetLabel) {
    return new BranchInstruction(IROpcode.BRANCH_FALSE, null, null).addOperands(condition, targetLabel);
  }

  /**
   * Create conditional branch (false) with debug info: BRANCH_FALSE condition, target_label
   */
  public static BranchInstruction branchFalse(final String condition, final String targetLabel, final DebugInfo debugInfo) {
    return new BranchInstruction(IROpcode.BRANCH_FALSE, null, debugInfo).addOperands(condition, targetLabel);
  }

  /**
   * Create return with no value: RETURN
   */
  public static BranchInstruction returnVoid() {
    return new BranchInstruction(IROpcode.RETURN, null, null);
  }

  /**
   * Create return with no value and debug info: RETURN
   */
  public static BranchInstruction returnVoid(final DebugInfo debugInfo) {
    return new BranchInstruction(IROpcode.RETURN, null, debugInfo);
  }

  /**
   * Create return with value: RETURN value
   */
  public static BranchInstruction returnValue(final String value) {
    return new BranchInstruction(IROpcode.RETURN, null, null).addOperand(value);
  }

  /**
   * Create return with value and debug info: RETURN value
   */
  public static BranchInstruction returnValue(final String value, final DebugInfo debugInfo) {
    return new BranchInstruction(IROpcode.RETURN, null, debugInfo).addOperand(value);
  }

  private BranchInstruction(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public BranchInstruction addOperand(final String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public BranchInstruction addOperands(final String... operands) {
    super.addOperands(operands);
    return this;
  }

  /**
   * Get target label for branch instructions.
   */
  public java.lang.String getTargetLabel() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.BRANCH) {
      return operands.isEmpty() ? null : operands.get(0);
    } else if (getOpcode() == IROpcode.BRANCH_TRUE || getOpcode() == IROpcode.BRANCH_FALSE) {
      return operands.size() < 2 ? null : operands.get(1);
    }
    return null;
  }

  /**
   * Get condition for conditional branch instructions.
   */
  public java.lang.String getCondition() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.BRANCH_TRUE || getOpcode() == IROpcode.BRANCH_FALSE) {
      return operands.isEmpty() ? null : operands.get(0);
    }
    return null;
  }

  /**
   * Get return value for RETURN instructions.
   */
  public java.lang.String getReturnValue() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.RETURN) {
      return operands.isEmpty() ? null : operands.get(0);
    }
    return null;
  }
}