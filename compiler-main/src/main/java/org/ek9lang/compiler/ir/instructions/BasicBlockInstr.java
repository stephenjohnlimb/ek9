package org.ek9lang.compiler.ir.instructions;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.compiler.common.INodeVisitor;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.core.AssertValue;

/**
 * Represents a basic block in the EK9 IR - a sequence of instructions with single entry and exit.
 * <p>
 * Basic blocks are fundamental units of control flow analysis and optimization.
 * Each block contains a linear sequence of instructions ending with a control flow instruction.
 * </p>
 */
public final class BasicBlockInstr implements INode {

  private final String label;
  private final List<IRInstr> instructions = new ArrayList<>();
  private final List<BasicBlockInstr> predecessors = new ArrayList<>();
  private final List<BasicBlockInstr> successors = new ArrayList<>();

  /**
   * Create basic block with given label.
   */
  public BasicBlockInstr(final String label) {
    AssertValue.checkNotNull("Label cannot be null", label);
    this.label = label;
  }

  /**
   * Add instruction to this basic block.
   */
  public void addInstruction(final IRInstr instruction) {
    AssertValue.checkNotNull("Instruction cannot be null", instruction);
    instructions.add(instruction);
  }

  /**
   * Add multiple instructions to this basic block.
   */
  public BasicBlockInstr addInstructions(final IRInstr... instructions) {
    for (IRInstr instruction : instructions) {
      addInstruction(instruction);
    }
    return this;
  }

  /**
   * Add multiple instructions from a list.
   */
  public void addInstructions(final List<IRInstr> instructions) {
    for (IRInstr instruction : instructions) {
      addInstruction(instruction);
    }
  }

  /**
   * Add successor basic block (control flow edge).
   */
  public BasicBlockInstr addSuccessor(final BasicBlockInstr successor) {
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

  public List<IRInstr> getInstructions() {
    return List.copyOf(instructions);
  }

  public List<BasicBlockInstr> getPredecessors() {
    return List.copyOf(predecessors);
  }

  public List<BasicBlockInstr> getSuccessors() {
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
  public IRInstr getLastInstruction() {
    return instructions.isEmpty() ? null : instructions.getLast();
  }

  /**
   * Check if this block ends with a control flow instruction.
   */
  public boolean endsWithControlFlow() {
    IRInstr last = getLastInstruction();
    return last != null && last.isControlFlow();
  }

  /**
   * Check if this block is a terminator (ends with RETURN).
   */
  public boolean isTerminator() {
    IRInstr last = getLastInstruction();
    return last != null && last.getOpcode() == IROpcode.RETURN;
  }

  /**
   * Get all method call instructions in this block.
   */
  public List<IRInstr> getMethodCalls() {
    return instructions.stream()
        .filter(IRInstr::isMethodCall)
        .toList();
  }

  /**
   * Get all memory management instructions in this block.
   */
  public List<IRInstr> getMemoryManagementInstructions() {
    return instructions.stream()
        .filter(IRInstr::isMemoryManagement)
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

    for (IRInstr instruction : instructions) {
      sb.append("  ").append(instruction).append("\n");
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

    BasicBlockInstr that = (BasicBlockInstr) obj;
    return label.equals(that.label);
  }

  @Override
  public int hashCode() {
    return label.hashCode();
  }
}