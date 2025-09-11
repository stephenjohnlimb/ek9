package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
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

  private final ConditionalEvaluation conditionalEvaluation;
  private final OperandEvaluation assignmentEvaluation;
  private final String scopeId;

  /**
   * Create guarded assignment block.
   */
  public static GuardedAssignmentBlockInstr guardedAssignmentBlock(final ConditionalEvaluation conditionalEvaluation,
                                                                   final OperandEvaluation assignmentEvaluation,
                                                                   final BasicDetails basicDetails) {
    return new GuardedAssignmentBlockInstr(conditionalEvaluation, assignmentEvaluation, basicDetails);
  }

  private GuardedAssignmentBlockInstr(final ConditionalEvaluation conditionalEvaluation,
                                      final OperandEvaluation assignmentEvaluation,
                                      final BasicDetails basicDetails) {
    super(IROpcode.GUARDED_ASSIGNMENT_BLOCK, null, basicDetails.debugInfo());

    AssertValue.checkNotNull("Conditional evaluation cannot be null", conditionalEvaluation);
    AssertValue.checkNotNull("Assignment evaluation cannot be null", assignmentEvaluation);
    AssertValue.checkNotNull("Scope ID cannot be null", basicDetails.scopeId());

    this.conditionalEvaluation = conditionalEvaluation;
    this.assignmentEvaluation = assignmentEvaluation;
    this.scopeId = basicDetails.scopeId();
  }

  /**
   * Get instructions for evaluating the assignment condition (should assignment occur?).
   */
  public List<IRInstr> getConditionEvaluationInstructions() {
    return conditionalEvaluation.conditionInstructions();
  }

  /**
   * Get the condition result variable name (Boolean indicating if assignment should occur).
   */
  public String getConditionResult() {
    return conditionalEvaluation.conditionResult();
  }

  /**
   * Get instructions for performing the assignment (when condition is true).
   */
  public List<IRInstr> getAssignmentEvaluationInstructions() {
    return assignmentEvaluation.evaluationInstructions();
  }

  /**
   * Get the assignment result variable name.
   */
  public String getAssignmentResult() {
    return assignmentEvaluation.operandName();
  }

  /**
   * Get the scope ID for memory management.
   */
  public String getScopeId() {
    return scopeId;
  }

  /**
   * Get the conditional evaluation record.
   */
  public ConditionalEvaluation getConditionalEvaluation() {
    return conditionalEvaluation;
  }

  /**
   * Get the assignment evaluation record.
   */
  public OperandEvaluation getAssignmentEvaluation() {
    return assignmentEvaluation;
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
    if (conditionalEvaluation.conditionInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : conditionalEvaluation.conditionInstructions()) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("condition_result: ").append(conditionalEvaluation.conditionResult()).append("\n");

    // Assignment evaluation section
    sb.append("assignment_evaluation:\n[\n");
    if (assignmentEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : assignmentEvaluation.evaluationInstructions()) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("assignment_result: ").append(assignmentEvaluation.operandName()).append("\n");
    sb.append("scope_id: ").append(scopeId).append("\n");

    sb.append("]");

    return sb.toString();
  }
}