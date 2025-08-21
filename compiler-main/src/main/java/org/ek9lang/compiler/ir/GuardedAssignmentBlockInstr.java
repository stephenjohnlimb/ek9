package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.core.AssertValue;

/**
 * IR instruction for guarded assignment block (:=? operator) with null-safe capability.
 * <p>
 * This high-level instruction provides a declarative approach to conditional assignment,
 * allowing backends to choose between branching and conditional selection strategies:
 * - LLVM: Lower to conditional select or branches based on optimization context
 * - JVM: Lower to bytecode jumps or conditional expressions
 * - Other backends: Optimize based on target-specific capabilities
 * </p>
 * <p>
 * Contains complete evaluation paths for condition checking and assignment execution.
 * Backends can use this information to make optimal code generation decisions.
 * </p>
 * <p>
 * Assignment Logic: Assign only if LHS is null OR LHS._isSet() == false
 * Uses QUESTION_BLOCK semantics internally for consistent null-safety behavior.
 * </p>
 * <p>
 * Format: GUARDED_ASSIGNMENT_BLOCK result = condition_evaluation, assignment_evaluation
 * </p>
 */
public final class GuardedAssignmentBlockInstr extends IRInstr {

  private final List<IRInstr> conditionEvaluationInstructions;
  private final String conditionResult;
  private final List<IRInstr> assignmentEvaluationInstructions;
  private final String assignmentResult;
  private final String scopeId;

  /**
   * Create guarded assignment block.
   */
  public static GuardedAssignmentBlockInstr guardedAssignmentBlock(final String result,
                                                                   final List<IRInstr> conditionEvaluationInstructions,
                                                                   final String conditionResult,
                                                                   final List<IRInstr> assignmentEvaluationInstructions,
                                                                   final String assignmentResult,
                                                                   final BasicDetails basicDetails) {
    return new GuardedAssignmentBlockInstr(result, conditionEvaluationInstructions, conditionResult,
        assignmentEvaluationInstructions, assignmentResult, basicDetails);
  }

  private GuardedAssignmentBlockInstr(final String result,
                                      final List<IRInstr> conditionEvaluationInstructions,
                                      final String conditionResult,
                                      final List<IRInstr> assignmentEvaluationInstructions,
                                      final String assignmentResult,
                                      final BasicDetails basicDetails) {
    super(IROpcode.GUARDED_ASSIGNMENT_BLOCK, result, basicDetails.debugInfo());

    AssertValue.checkNotNull("Condition evaluation instructions cannot be null", conditionEvaluationInstructions);
    AssertValue.checkNotNull("Condition result cannot be null", conditionResult);
    AssertValue.checkNotNull("Assignment evaluation instructions cannot be null", assignmentEvaluationInstructions);
    AssertValue.checkNotNull("Assignment result cannot be null", assignmentResult);
    AssertValue.checkNotNull("Scope ID cannot be null", basicDetails.scopeId());

    this.conditionEvaluationInstructions = conditionEvaluationInstructions;
    this.conditionResult = conditionResult;
    this.assignmentEvaluationInstructions = assignmentEvaluationInstructions;
    this.assignmentResult = assignmentResult;
    this.scopeId = basicDetails.scopeId();
  }

  /**
   * Get instructions for evaluating the assignment condition (should assignment occur?).
   */
  public List<IRInstr> getConditionEvaluationInstructions() {
    return conditionEvaluationInstructions;
  }

  /**
   * Get the condition result variable name (Boolean indicating if assignment should occur).
   */
  public String getConditionResult() {
    return conditionResult;
  }

  /**
   * Get instructions for performing the assignment (when condition is true).
   */
  public List<IRInstr> getAssignmentEvaluationInstructions() {
    return assignmentEvaluationInstructions;
  }

  /**
   * Get the assignment result variable name.
   */
  public String getAssignmentResult() {
    return assignmentResult;
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

    // Condition evaluation section
    sb.append("condition_evaluation:\n[\n");
    if (conditionEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : conditionEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("condition_result: ").append(conditionResult).append("\n");

    // Assignment evaluation section
    sb.append("assignment_evaluation:\n[\n");
    if (assignmentEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : assignmentEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("assignment_result: ").append(assignmentResult).append("\n");
    sb.append("scope_id: ").append(scopeId).append("\n");

    sb.append("]");

    return sb.toString();
  }
}