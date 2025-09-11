package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.data.LogicalDetails;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.LogicalOperationContext;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
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

  private final LogicalOperationContext operationContext;
  private final OperandEvaluation leftEvaluation;
  private final ConditionalEvaluation conditionalEvaluation;
  private final OperandEvaluation rightEvaluation;
  private final OperandEvaluation resultEvaluation;
  private final String scopeId;

  /**
   * Create logical AND operation.
   */
  public static IRInstr andOperation(final LogicalDetails logicalDetails) {
    final var operationContext = new LogicalOperationContext(IROpcode.LOGICAL_AND_BLOCK,
        logicalDetails.result(), Operation.AND);
    return new LogicalOperationInstr(operationContext, logicalDetails);
  }

  /**
   * Create logical OR operation.
   */
  public static IRInstr orOperation(final LogicalDetails logicalDetails) {
    final var operationContext = new LogicalOperationContext(IROpcode.LOGICAL_OR_BLOCK,
        logicalDetails.result(), Operation.OR);
    return new LogicalOperationInstr(operationContext, logicalDetails);
  }

  private LogicalOperationInstr(final LogicalOperationContext operationContext,
                                final LogicalDetails logicalDetails) {
    super(operationContext.opcode(), operationContext.result(), logicalDetails.basicDetails().debugInfo());

    AssertValue.checkNotNull("OperationInstr context cannot be null", operationContext);
    AssertValue.checkNotNull("logicalDetails evaluation cannot be null", logicalDetails);

    this.operationContext = operationContext;
    this.leftEvaluation = logicalDetails.leftEvaluation();
    this.conditionalEvaluation = logicalDetails.conditionalEvaluation();
    this.rightEvaluation = logicalDetails.rightEvaluation();
    this.resultEvaluation = logicalDetails.resultEvaluation();
    this.scopeId = logicalDetails.basicDetails().scopeId();

    // Store operands for base class functionality
    addOperand(leftEvaluation.operandName());
    addOperand(conditionalEvaluation.conditionResult());
    addOperand(rightEvaluation.operandName());
    addOperand(resultEvaluation.operandName());
    addOperand(scopeId);
  }

  /**
   * Get the operation type (AND or OR).
   */
  public Operation getOperation() {
    return operationContext.operation();
  }

  /**
   * Get the left operand evaluation instructions.
   * These instructions evaluate the left EK9 Boolean operand.
   */
  public List<IRInstr> getLeftEvaluationInstructions() {
    return leftEvaluation.evaluationInstructions();
  }

  /**
   * Get the left operand variable name.
   */
  public String getLeftOperand() {
    return leftEvaluation.operandName();
  }

  /**
   * Get the primitive boolean condition variable name.
   */
  public String getCondition() {
    return conditionalEvaluation.conditionResult();
  }

  /**
   * Get the right operand evaluation instructions.
   * These instructions evaluate the right EK9 Boolean operand.
   */
  public List<IRInstr> getRightEvaluationInstructions() {
    return rightEvaluation.evaluationInstructions();
  }

  /**
   * Get the variable name that holds the right operand EK9 Boolean.
   */
  public String getRightOperand() {
    return rightEvaluation.operandName();
  }

  /**
   * Get the result computation instructions.
   * These instructions compute the logical AND/OR operation result.
   */
  public List<IRInstr> getResultComputationInstructions() {
    return resultEvaluation.evaluationInstructions();
  }

  /**
   * Get the variable name that holds the logical operation result.
   */
  public String getLogicalResult() {
    return resultEvaluation.operandName();
  }

  /**
   * Get the scope ID for memory management.
   */
  public String getScopeId() {
    return scopeId;
  }

  /**
   * Get the left operand evaluation record.
   */
  public OperandEvaluation getLeftEvaluation() {
    return leftEvaluation;
  }

  /**
   * Get the conditional evaluation record.
   */
  public ConditionalEvaluation getConditionalEvaluation() {
    return conditionalEvaluation;
  }

  /**
   * Get the right operand evaluation record.
   */
  public OperandEvaluation getRightEvaluation() {
    return rightEvaluation;
  }

  /**
   * Get the result evaluation record.
   */
  public OperandEvaluation getResultEvaluation() {
    return resultEvaluation;
  }

  /**
   * Get the logical operation context record.
   */
  public LogicalOperationContext getOperationContext() {
    return operationContext;
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
    if (leftEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : leftEvaluation.evaluationInstructions()) {
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
    sb.append("left_operand: ").append(leftEvaluation.operandName()).append("\n");
    sb.append("left_condition: ").append(conditionalEvaluation.conditionResult()).append("\n");

    // Right operand evaluation section
    sb.append("right_evaluation:\n[\n");
    if (rightEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : rightEvaluation.evaluationInstructions()) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("right_operand: ").append(rightEvaluation.operandName()).append("\n");

    // Result computation section
    sb.append("result_computation:\n[\n");
    if (resultEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : resultEvaluation.evaluationInstructions()) {
        sb.append(instr.toString());
        // Add newline if instruction doesn't already end with one
        if (!instr.toString().endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("logical_result: ").append(resultEvaluation.operandName()).append("\n");
    sb.append("scope_id: ").append(scopeId).append("\n");

    sb.append("]");

    return sb.toString();
  }
}