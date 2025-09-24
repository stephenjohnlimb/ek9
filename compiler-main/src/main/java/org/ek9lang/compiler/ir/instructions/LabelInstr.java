package org.ek9lang.compiler.ir.instructions;

import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Specialized IR instruction for control flow labels (LABEL).
 * <p>
 * Label instructions mark specific instruction positions as branch targets
 * for control flow operations like BRANCH_TRUE, BRANCH_FALSE, and BRANCH.
 * </p>
 */
public final class LabelInstr extends IRInstr {

  /**
   * Create label instruction: LABEL label_name.
   */
  public static LabelInstr label(final String labelName) {
    return new LabelInstr(IROpcode.LABEL, null, null).addOperand(labelName);
  }

  /**
   * Create label instruction with debug info: LABEL label_name.
   */
  public static LabelInstr label(final String labelName, final DebugInfo debugInfo) {
    return new LabelInstr(IROpcode.LABEL, null, debugInfo).addOperand(labelName);
  }

  private LabelInstr(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public LabelInstr addOperand(final String operand) {
    super.addOperand(operand);
    return this;
  }

  /**
   * Get the label name for this label instruction.
   */
  public String getLabelName() {
    final var operands = getOperands();
    return operands.isEmpty() ? null : operands.getFirst();
  }

  @Override
  public String toString() {
    final var labelName = getLabelName();
    StringBuilder sb = new StringBuilder();

    sb.append("LABEL");
    if (labelName != null) {
      sb.append(" ").append(labelName);
    }

    // Add debug information as comment if available
    if (getDebugInfo().isPresent() && getDebugInfo().get().isValidLocation()) {
      sb.append("  ").append(getDebugInfo().get());
    }

    return sb.toString();
  }
}