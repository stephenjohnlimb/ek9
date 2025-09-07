package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * Base class for all IR instructions in the EK9 intermediate representation.
 * <p>
 * Each instruction represents a single operation in the target-agnostic IR,
 * which can be translated to JVM bytecode, LLVM IR, or other target formats.
 * </p>
 * <p>
 * The subclasses from this calls will actually hold the appropriate
 * details accessible via method calls. The idea is not to have to parse or
 * process these operands. Its just very useful to see a human-readable form
 * when debugging and also for testing IR output.
 * </p>
 */
public class IRInstr implements INode {

  private final IROpcode opcode;
  //Note these are really aimed as enabling human visualisation of the call.
  private final List<String> operands = new ArrayList<>();
  private final String result;

  // Optional debug information for source mapping and debugging
  private final DebugInfo debugInfo;
  
  // Optional escape analysis metadata for optimization (set by IR_OPTIMISATION phase)
  private EscapeMetaData escapeMetaData;

  /**
   * Create instruction with no result (e.g., STORE, BRANCH).
   */
  public IRInstr(final IROpcode opcode) {
    this(opcode, null, null);
  }

  /**
   * Create instruction with result destination (e.g., LOAD, CALL).
   */
  public IRInstr(final IROpcode opcode, final String result) {
    this(opcode, result, null);
  }

  /**
   * Create instruction with result and debug information.
   */
  public IRInstr(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    AssertValue.checkNotNull("Opcode cannot be null", opcode);
    this.opcode = opcode;
    this.result = result;
    this.debugInfo = debugInfo;
  }

  /**
   * Add operand to this instruction.
   */
  public IRInstr addOperand(final String operand) {
    AssertValue.checkNotNull("Operand cannot be null", operand);
    operands.add(operand);
    return this;
  }

  /**
   * Add multiple operands to this instruction.
   */
  public IRInstr addOperands(final String... operands) {
    for (String operand : operands) {
      addOperand(operand);
    }
    return this;
  }

  public IROpcode getOpcode() {
    return opcode;
  }

  public String getResult() {
    return result;
  }

  public List<String> getOperands() {
    return List.copyOf(operands);
  }

  /**
   * Get debug information for this instruction.
   */
  public Optional<DebugInfo> getDebugInfo() {
    return Optional.ofNullable(debugInfo);
  }

  /**
   * Get escape analysis metadata for this instruction.
   */
  public Optional<EscapeMetaData> getEscapeMetaData() {
    return Optional.ofNullable(escapeMetaData);
  }

  /**
   * Set escape analysis metadata for this instruction.
   * Should only be called during IR_OPTIMISATION phase.
   */
  public void setEscapeMetaData(final EscapeMetaData escapeMetaData) {
    this.escapeMetaData = escapeMetaData;
  }

  /**
   * Check if this instruction has escape analysis metadata.
   */
  public boolean hasEscapeMetaData() {
    return escapeMetaData != null;
  }

  /**
   * Check if this instruction produces a result value.
   */
  public boolean hasResult() {
    return result != null;
  }

  /**
   * Check if this instruction is a method call type.
   */
  public boolean isMethodCall() {
    return opcode == IROpcode.CALL
        || opcode == IROpcode.CALL_VIRTUAL
        || opcode == IROpcode.CALL_STATIC
        || opcode == IROpcode.CALL_DISPATCHER;
  }

  /**
   * Check if this instruction is a control flow type.
   */
  public boolean isControlFlow() {
    return opcode == IROpcode.BRANCH
        || opcode == IROpcode.BRANCH_TRUE
        || opcode == IROpcode.BRANCH_FALSE
        || opcode == IROpcode.RETURN;
  }

  /**
   * Check if this instruction is a memory management type.
   */
  public boolean isMemoryManagement() {
    return opcode == IROpcode.RETAIN
        || opcode == IROpcode.RELEASE
        || opcode == IROpcode.SCOPE_ENTER
        || opcode == IROpcode.SCOPE_EXIT
        || opcode == IROpcode.SCOPE_REGISTER;
  }

  /**
   * Check if this instruction is a label marker for control flow.
   */
  public boolean isLabel() {
    return opcode == IROpcode.LABEL;
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    // Format: [RESULT =] OPCODE operand1, operand2, ... [// debug_info]
    if (hasResult()) {
      sb.append(result).append(" = ");
    }

    sb.append(opcode);

    if (!operands.isEmpty()) {
      sb.append(" ");
      sb.append(String.join(", ", operands));
    }

    // Add escape metadata if available
    if (escapeMetaData != null) {
      sb.append(" ").append(escapeMetaData);
    }

    // Add debug information as comment if available
    if (debugInfo != null && debugInfo.isValidLocation()) {
      sb.append("  ").append(debugInfo);
    }

    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    IRInstr that = (IRInstr) obj;
    return opcode == that.opcode
        && java.util.Objects.equals(result, that.result)
        && operands.equals(that.operands);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(opcode, result, operands);
  }
}