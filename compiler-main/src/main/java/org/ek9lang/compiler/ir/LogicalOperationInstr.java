package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.core.AssertValue;

/**
 * IR instruction for logical operations (AND/OR) with short-circuit capability.
 * <p>
 * This high-level instruction provides a declarative approach to Boolean operations,
 * allowing backends to choose between short-circuit and full evaluation strategies:
 * - LLVM: Lower to conditional branches with PHI nodes
 * - JVM: Lower to bytecode jumps or full method calls
 * - Other backends: Optimize based on target-specific capabilities
 * </p>
 * <p>
 * Contains complete evaluation paths for both operands and the logical result.
 * Backends can use the primitive boolean condition to make short-circuit decisions.
 * </p>
 * <p>
 * Format: LOGICAL_AND_BLOCK/OR_BLOCK result = left_operand, condition, right_evaluation, logical_result
 * </p>
 */
public final class LogicalOperationInstr extends IRInstr {

  /**
   * The type of logical operation.
   */
  public enum Operation {
    AND, OR
  }

  private final Operation operation;
  private final List<IRInstr> leftEvaluationInstructions;
  private final String leftOperand;
  private final String condition;
  private final List<IRInstr> rightEvaluationInstructions;
  private final String rightOperand;
  private final List<IRInstr> resultComputationInstructions;
  private final String logicalResult;
  private final String scopeId;

  /**
   * Create logical AND operation.
   */
  public static LogicalOperationInstr andOperation(final String result,
                                                   final List<IRInstr> leftEvaluationInstructions,
                                                   final String leftOperand,
                                                   final String condition,
                                                   final List<IRInstr> rightEvaluationInstructions,
                                                   final String rightOperand,
                                                   final List<IRInstr> resultComputationInstructions,
                                                   final String logicalResult,
                                                   BasicDetails basicDetails) {
    return new LogicalOperationInstr(IROpcode.LOGICAL_AND_BLOCK, result, Operation.AND,
        leftEvaluationInstructions, leftOperand, condition, rightEvaluationInstructions, rightOperand,
        resultComputationInstructions, logicalResult, basicDetails);
  }

  /**
   * Create logical OR operation.
   */
  public static LogicalOperationInstr orOperation(final String result,
                                                  final List<IRInstr> leftEvaluationInstructions,
                                                  final String leftOperand,
                                                  final String condition,
                                                  final List<IRInstr> rightEvaluationInstructions,
                                                  final String rightOperand,
                                                  final List<IRInstr> resultComputationInstructions,
                                                  final String logicalResult,
                                                  BasicDetails basicDetails) {
    return new LogicalOperationInstr(IROpcode.LOGICAL_OR_BLOCK, result, Operation.OR,
        leftEvaluationInstructions, leftOperand, condition, rightEvaluationInstructions, rightOperand,
        resultComputationInstructions, logicalResult, basicDetails);
  }

  private LogicalOperationInstr(final IROpcode opcode,
                                final String result,
                                final Operation operation,
                                final List<IRInstr> leftEvaluationInstructions,
                                final String leftOperand,
                                final String condition,
                                final List<IRInstr> rightEvaluationInstructions,
                                final String rightOperand,
                                final List<IRInstr> resultComputationInstructions,
                                final String logicalResult,
                                BasicDetails basicDetails) {
    super(opcode, result, basicDetails.debugInfo());

    AssertValue.checkNotNull("Operation cannot be null", operation);
    AssertValue.checkNotNull("Left evaluation instructions cannot be null", leftEvaluationInstructions);
    AssertValue.checkNotNull("Left operand cannot be null", leftOperand);
    AssertValue.checkNotNull("Condition cannot be null", condition);
    AssertValue.checkNotNull("Right evaluation instructions cannot be null", rightEvaluationInstructions);
    AssertValue.checkNotNull("Right operand cannot be null", rightOperand);
    AssertValue.checkNotNull("Result computation instructions cannot be null", resultComputationInstructions);
    AssertValue.checkNotNull("Logical result cannot be null", logicalResult);
    AssertValue.checkNotNull("Scope ID cannot be null", basicDetails.scopeId());

    this.operation = operation;
    this.leftEvaluationInstructions = List.copyOf(leftEvaluationInstructions);
    this.leftOperand = leftOperand;
    this.condition = condition;
    this.rightEvaluationInstructions = List.copyOf(rightEvaluationInstructions);
    this.rightOperand = rightOperand;
    this.resultComputationInstructions = List.copyOf(resultComputationInstructions);
    this.logicalResult = logicalResult;
    this.scopeId = basicDetails.scopeId();

    // Store operands for base class functionality
    addOperand(leftOperand);
    addOperand(condition);
    addOperand(rightOperand);
    addOperand(logicalResult);
    addOperand(scopeId);
  }

  /**
   * Get the operation type (AND or OR).
   */
  public Operation getOperation() {
    return operation;
  }

  /**
   * Get the left operand evaluation instructions.
   * These instructions evaluate the left EK9 Boolean operand.
   */
  public List<IRInstr> getLeftEvaluationInstructions() {
    return leftEvaluationInstructions;
  }

  /**
   * Get the left operand variable name.
   */
  public String getLeftOperand() {
    return leftOperand;
  }

  /**
   * Get the primitive boolean condition variable name.
   */
  public String getCondition() {
    return condition;
  }

  /**
   * Get the right operand evaluation instructions.
   * These instructions evaluate the right EK9 Boolean operand.
   */
  public List<IRInstr> getRightEvaluationInstructions() {
    return rightEvaluationInstructions;
  }

  /**
   * Get the variable name that holds the right operand EK9 Boolean.
   */
  public String getRightOperand() {
    return rightOperand;
  }

  /**
   * Get the result computation instructions.
   * These instructions compute the logical AND/OR operation result.
   */
  public List<IRInstr> getResultComputationInstructions() {
    return resultComputationInstructions;
  }

  /**
   * Get the variable name that holds the logical operation result.
   */
  public String getLogicalResult() {
    return logicalResult;
  }

  /**
   * Get the scope ID for memory management.
   */
  public String getScopeId() {
    return scopeId;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (getResult() != null) {
      sb.append(getResult()).append(" = ");
    }

    sb.append(getOpcode().name());

    // Add debug information as comment if available
    if (getDebugInfo().isPresent() && getDebugInfo().get().isValidLocation()) {
      sb.append("  ").append(getDebugInfo().get());
    }
    sb.append("\n[\n");

    // Left operand evaluation section
    sb.append("left_evaluation:\n[\n");
    if (leftEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : leftEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");

    // Left operand section
    sb.append("left_operand: ").append(leftOperand).append("\n");
    sb.append("left_condition: ").append(condition).append("\n");

    // Right operand evaluation section
    sb.append("right_evaluation:\n[\n");
    if (rightEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : rightEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("right_operand: ").append(rightOperand).append("\n");

    // Result computation section
    sb.append("result_computation:\n[\n");
    if (resultComputationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : resultComputationInstructions) {
        sb.append(instr.toString());
        // Add newline if instruction doesn't already end with one
        if (!instr.toString().endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("logical_result: ").append(logicalResult).append("\n");
    sb.append("scope_id: ").append(scopeId).append("\n");

    sb.append("]");

    return sb.toString();
  }
}