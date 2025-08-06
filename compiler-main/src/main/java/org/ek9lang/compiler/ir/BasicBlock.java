package org.ek9lang.compiler.ir;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.core.AssertValue;

/**
 * Represents a basic block in the EK9 IR - a sequence of instructions with single entry and exit.
 * <p>
 * Basic blocks are fundamental units of control flow analysis and optimization.
 * Each block contains a linear sequence of instructions ending with a control flow instruction.
 * </p>
 */
public final class BasicBlock implements INode {

  private final String label;
  private final List<IRInstruction> instructions = new ArrayList<>();
  private final List<BasicBlock> predecessors = new ArrayList<>();
  private final List<BasicBlock> successors = new ArrayList<>();

  /**
   * Create basic block with given label.
   */
  public BasicBlock(final String label) {
    AssertValue.checkNotNull("Label cannot be null", label);
    this.label = label;
  }

  /**
   * Add instruction to this basic block.
   */
  public BasicBlock addInstruction(final IRInstruction instruction) {
    AssertValue.checkNotNull("Instruction cannot be null", instruction);
    instructions.add(instruction);
    return this;
  }

  /**
   * Add multiple instructions to this basic block.
   */
  public BasicBlock addInstructions(final IRInstruction... instructions) {
    for (IRInstruction instruction : instructions) {
      addInstruction(instruction);
    }
    return this;
  }

  /**
   * Add multiple instructions from a list.
   */
  public BasicBlock addInstructions(final List<IRInstruction> instructions) {
    for (IRInstruction instruction : instructions) {
      addInstruction(instruction);
    }
    return this;
  }

  /**
   * Add successor basic block (control flow edge).
   */
  public BasicBlock addSuccessor(final BasicBlock successor) {
    AssertValue.checkNotNull("Successor cannot be null", successor);
    if (!successors.contains(successor)) {
      successors.add(successor);
      successor.predecessors.add(this);
    }
    return this;
  }

  public String getLabel() {
    return label;
  }

  public List<IRInstruction> getInstructions() {
    return List.copyOf(instructions);
  }

  public List<BasicBlock> getPredecessors() {
    return List.copyOf(predecessors);
  }

  public List<BasicBlock> getSuccessors() {
    return List.copyOf(successors);
  }

  /**
   * Check if this block is empty (no instructions).
   */
  public boolean isEmpty() {
    return instructions.isEmpty();
  }

  /**
   * Get the last instruction in this block (usually control flow).
   */
  public IRInstruction getLastInstruction() {
    return instructions.isEmpty() ? null : instructions.get(instructions.size() - 1);
  }

  /**
   * Check if this block ends with a control flow instruction.
   */
  public boolean endsWithControlFlow() {
    IRInstruction last = getLastInstruction();
    return last != null && last.isControlFlow();
  }

  /**
   * Check if this block is a terminator (ends with RETURN).
   */
  public boolean isTerminator() {
    IRInstruction last = getLastInstruction();
    return last != null && last.getOpcode() == IROpcode.RETURN;
  }

  /**
   * Get all method call instructions in this block.
   */
  public List<IRInstruction> getMethodCalls() {
    return instructions.stream()
        .filter(IRInstruction::isMethodCall)
        .toList();
  }

  /**
   * Get all memory management instructions in this block.
   */
  public List<IRInstruction> getMemoryManagementInstructions() {
    return instructions.stream()
        .filter(IRInstruction::isMemoryManagement)
        .toList();
  }

  @Override
  public void accept(final INodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append(label).append(":\n");
    
    for (IRInstruction instruction : instructions) {
      sb.append("  ").append(instruction).append("\n");
    }
    
    return sb.toString();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;
    
    BasicBlock that = (BasicBlock) obj;
    return label.equals(that.label);
  }

  @Override
  public int hashCode() {
    return label.hashCode();
  }
}