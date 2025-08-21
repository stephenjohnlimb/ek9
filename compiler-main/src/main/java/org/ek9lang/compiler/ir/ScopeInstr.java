package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.phase7.support.BasicDetails;

/**
 * Specialized IR instruction for scope management (SCOPE_ENTER, SCOPE_EXIT, SCOPE_REGISTER).
 * <p>
 * Scope instructions provide exception-safe memory management for EK9, via ARC.
 * </p>
 */
public final class ScopeInstr extends IRInstr {

  /**
   * Create scope enter instruction with debug info: SCOPE_ENTER scope_id.
   */
  public static ScopeInstr enter(final String scopeId, final DebugInfo debugInfo) {
    return new ScopeInstr(IROpcode.SCOPE_ENTER, null, debugInfo).addOperand(scopeId);
  }

  /**
   * Create scope exit instruction with debug info: SCOPE_EXIT scope_id.
   */
  public static ScopeInstr exit(final String scopeId, final DebugInfo debugInfo) {
    return new ScopeInstr(IROpcode.SCOPE_EXIT, null, debugInfo).addOperand(scopeId);
  }

  /**
   * Create scope register instruction with debug info: SCOPE_REGISTER object, scope_id.
   */
  public static ScopeInstr register(final String object, final BasicDetails basicDetails) {
    return new ScopeInstr(IROpcode.SCOPE_REGISTER, null, basicDetails.debugInfo())
        .addOperands(object, basicDetails.scopeId());
  }

  private ScopeInstr(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public ScopeInstr addOperand(final java.lang.String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public ScopeInstr addOperands(final java.lang.String... operands) {
    super.addOperands(operands);
    return this;
  }

  /**
   * Get scope ID for this scope instruction.
   */
  public java.lang.String getScopeId() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.SCOPE_ENTER || getOpcode() == IROpcode.SCOPE_EXIT) {
      return operands.isEmpty() ? null : operands.getFirst();
    } else if (getOpcode() == IROpcode.SCOPE_REGISTER) {
      return operands.size() < 2 ? null : operands.getLast();
    }
    return null;
  }

  /**
   * Get object name for SCOPE_REGISTER instructions.
   */
  public java.lang.String getObject() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.SCOPE_REGISTER) {
      return operands.isEmpty() ? null : operands.getFirst();
    }
    return null;
  }

}