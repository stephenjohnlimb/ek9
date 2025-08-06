package org.ek9lang.compiler.ir;

import java.util.List;

/**
 * Specialized IR instruction for scope management (SCOPE_ENTER, SCOPE_EXIT, SCOPE_REGISTER).
 * <p>
 * Scope instructions provide exception-safe memory management for EK9.
 * They are no-ops on JVM (garbage collected) but generate cleanup code for LLVM targets.
 * </p>
 */
public final class ScopeInstruction extends IRInstruction {

  /**
   * Create scope enter instruction: SCOPE_ENTER scope_id
   * Establishes a new memory management scope for exception safety.
   */
  public static ScopeInstruction enter(final String scopeId) {
    return new ScopeInstruction(IROpcode.SCOPE_ENTER, null, null).addOperand(scopeId);
  }

  /**
   * Create scope enter instruction with debug info: SCOPE_ENTER scope_id
   */
  public static ScopeInstruction enter(final String scopeId, final DebugInfo debugInfo) {
    return new ScopeInstruction(IROpcode.SCOPE_ENTER, null, debugInfo).addOperand(scopeId);
  }

  /**
   * Create scope exit instruction: SCOPE_EXIT scope_id
   * Automatically RELEASE all objects registered in this scope.
   */
  public static ScopeInstruction exit(final String scopeId) {
    return new ScopeInstruction(IROpcode.SCOPE_EXIT, null, null).addOperand(scopeId);
  }

  /**
   * Create scope exit instruction with debug info: SCOPE_EXIT scope_id
   */
  public static ScopeInstruction exit(final String scopeId, final DebugInfo debugInfo) {
    return new ScopeInstruction(IROpcode.SCOPE_EXIT, null, debugInfo).addOperand(scopeId);
  }

  /**
   * Create scope register instruction: SCOPE_REGISTER object, scope_id
   * Register object for automatic cleanup when scope exits.
   */
  public static ScopeInstruction register(final String object, final String scopeId) {
    return new ScopeInstruction(IROpcode.SCOPE_REGISTER, null, null).addOperands(object, scopeId);
  }

  /**
   * Create scope register instruction with debug info: SCOPE_REGISTER object, scope_id
   */
  public static ScopeInstruction register(final String object, final String scopeId, final DebugInfo debugInfo) {
    return new ScopeInstruction(IROpcode.SCOPE_REGISTER, null, debugInfo).addOperands(object, scopeId);
  }

  private ScopeInstruction(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    super(opcode, result, debugInfo);
  }

  @Override
  public ScopeInstruction addOperand(final java.lang.String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public ScopeInstruction addOperands(final java.lang.String... operands) {
    super.addOperands(operands);
    return this;
  }

  /**
   * Get scope ID for this scope instruction.
   */
  public java.lang.String getScopeId() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.SCOPE_ENTER || getOpcode() == IROpcode.SCOPE_EXIT) {
      return operands.isEmpty() ? null : operands.get(0);
    } else if (getOpcode() == IROpcode.SCOPE_REGISTER) {
      return operands.size() < 2 ? null : operands.get(1);
    }
    return null;
  }

  /**
   * Get object name for SCOPE_REGISTER instructions.
   */
  public java.lang.String getObject() {
    List<java.lang.String> operands = getOperands();
    if (getOpcode() == IROpcode.SCOPE_REGISTER) {
      return operands.isEmpty() ? null : operands.get(0);
    }
    return null;
  }

}