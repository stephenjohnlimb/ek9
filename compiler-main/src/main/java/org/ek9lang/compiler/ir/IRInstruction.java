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
 */
public class IRInstruction implements INode {

  private final IROpcode opcode;
  private final List<String> operands = new ArrayList<>();
  private final String result;
  
  // Optional debug information for source mapping and debugging
  private final DebugInfo debugInfo;

  /**
   * Create instruction with no result (e.g., STORE, BRANCH).
   */
  public IRInstruction(final IROpcode opcode) {
    this(opcode, null, null);
  }

  /**
   * Create instruction with result destination (e.g., LOAD, CALL).
   */
  public IRInstruction(final IROpcode opcode, final String result) {
    this(opcode, result, null);
  }

  /**
   * Create instruction with result and debug information.
   */
  public IRInstruction(final IROpcode opcode, final String result, final DebugInfo debugInfo) {
    AssertValue.checkNotNull("Opcode cannot be null", opcode);
    this.opcode = opcode;
    this.result = result;
    this.debugInfo = debugInfo;
  }

  /**
   * Add operand to this instruction.
   */
  public IRInstruction addOperand(final String operand) {
    AssertValue.checkNotNull("Operand cannot be null", operand);
    operands.add(operand);
    return this;
  }

  /**
   * Add multiple operands to this instruction.
   */
  public IRInstruction addOperands(final String... operands) {
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
   * Check if this instruction produces a result value.
   */
  public boolean hasResult() {
    return result != null;
  }

  /**
   * Check if this instruction is a method call type.
   */
  public boolean isMethodCall() {
    return opcode == IROpcode.CALL || 
           opcode == IROpcode.CALL_VIRTUAL || 
           opcode == IROpcode.CALL_STATIC || 
           opcode == IROpcode.CALL_DISPATCHER;
  }

  /**
   * Check if this instruction is a control flow type.
   */
  public boolean isControlFlow() {
    return opcode == IROpcode.BRANCH || 
           opcode == IROpcode.BRANCH_TRUE || 
           opcode == IROpcode.BRANCH_FALSE || 
           opcode == IROpcode.RETURN;
  }

  /**
   * Check if this instruction is a memory management type.
   */
  public boolean isMemoryManagement() {
    return opcode == IROpcode.RETAIN || 
           opcode == IROpcode.RELEASE || 
           opcode == IROpcode.ALLOC_OBJECT ||
           opcode == IROpcode.SCOPE_ENTER ||
           opcode == IROpcode.SCOPE_EXIT ||
           opcode == IROpcode.SCOPE_REGISTER;
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
    
    // Add debug information as comment if available
    if (debugInfo != null && debugInfo.isValidLocation()) {
      sb.append("  ").append(debugInfo.toIRComment());
    }
    
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    
    IRInstruction that = (IRInstruction) obj;
    return opcode == that.opcode &&
           java.util.Objects.equals(result, that.result) &&
           operands.equals(that.operands);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(opcode, result, operands);
  }
}