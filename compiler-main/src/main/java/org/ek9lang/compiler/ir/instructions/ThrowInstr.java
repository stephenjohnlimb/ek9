package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Specialized IR instruction for throwing exceptions (THROW).
 * <p>
 * Handles both forms of throw statement:
 * - throw Exception("message") - constructor call expression
 * - throw exceptionVariable - variable reference
 * </p>
 * <p>
 * Key Design:
 * - Terminating instruction (like RETURN) - no code follows in same basic block
 * - Exception object MUST be on stack/in variable before throw
 * - Interacts with CONTROL_FLOW_CHAIN exception handlers for try/catch
 * - Backend generates ATHROW (JVM) or resume (LLVM) instruction
 * - Stack unwinding handled by backend exception mechanism
 * </p>
 * <p>
 * ARC Semantics:
 * - Exception object should be RETAINED and SCOPE_REGISTERED before throw
 * - THROW transfers ownership to exception mechanism (refcount maintained during unwinding)
 * - CATCH receives ownership without additional RETAIN (clean ownership transfer)
 * - SCOPE_EXIT after THROW is unreachable (backend handles cleanup during unwinding)
 * </p>
 */
public final class ThrowInstr extends IRInstr {

  /**
   * Create throw instruction with exception object variable and debug info.
   * The exception variable must contain a valid Exception or subtype object.
   *
   * @param exceptionVariable The variable containing the exception object to throw
   * @param debugInfo Debug information for source location (used in stack traces)
   * @return ThrowInstr instance
   */
  public static ThrowInstr throwException(final String exceptionVariable,
                                          final DebugInfo debugInfo) {
    return new ThrowInstr(exceptionVariable, debugInfo);
  }

  private ThrowInstr(final String exceptionVariable, final DebugInfo debugInfo) {
    super(IROpcode.THROW, null, debugInfo); // No result - terminates block
    super.addOperand(exceptionVariable);
  }

  /**
   * Get the exception object variable to throw.
   *
   * @return Exception variable name, or null if not set
   */
  public String getExceptionVariable() {
    final List<String> operands = getOperands();
    return operands.isEmpty() ? null : operands.getFirst();
  }

  @Override
  public ThrowInstr addOperand(final String operand) {
    super.addOperand(operand);
    return this;
  }

  @Override
  public ThrowInstr addOperands(final String... operands) {
    super.addOperands(operands);
    return this;
  }
}
