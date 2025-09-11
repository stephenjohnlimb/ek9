package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.data.QuestionDetails;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
import org.ek9lang.core.AssertValue;

/**
 * IR instruction for question operator (expression?) with null-safe capability.
 * <p>
 * This high-level instruction provides a declarative approach to null-safe _isSet() calls,
 * allowing backends to choose between branching and conditional selection strategies:
 * - LLVM: Lower to conditional select or branches based on optimization context
 * - JVM: Lower to bytecode jumps or conditional expressions
 * - Other backends: Optimize based on target-specific capabilities
 * </p>
 * <p>
 * Contains complete evaluation paths for both null and non-null cases.
 * Backends can use this information to make optimal code generation decisions.
 * </p>
 * <p>
 * Format: QUESTION_BLOCK result = operand_evaluation, null_case_evaluation, set_case_evaluation
 * </p>
 */
public final class QuestionOperatorInstr extends IRInstr {

  private final OperandEvaluation operandEvaluation;
  private final String nullCheckCondition;
  private final OperandEvaluation nullCaseEvaluation;
  private final OperandEvaluation setCaseEvaluation;
  private final String scopeId;

  /**
   * Create question operator block.
   */
  public static QuestionOperatorInstr questionBlock(final QuestionDetails questionDetails) {
    return new QuestionOperatorInstr(questionDetails);
  }

  private QuestionOperatorInstr(final QuestionDetails questionDetails) {
    super(IROpcode.QUESTION_BLOCK, questionDetails.result(), questionDetails.basicDetails().debugInfo());

    AssertValue.checkNotNull("questionDetails evaluation cannot be null", questionDetails);

    this.operandEvaluation = questionDetails.operandEvaluation();
    this.nullCheckCondition = questionDetails.nullCheckCondition();
    this.nullCaseEvaluation = questionDetails.nullCaseEvaluation();
    this.setCaseEvaluation = questionDetails.setCaseEvaluation();
    this.scopeId = questionDetails.basicDetails().scopeId();
  }

  /**
   * Get instructions for evaluating the operand (the expression that ? is applied to).
   */
  public List<IRInstr> getOperandEvaluationInstructions() {
    return operandEvaluation.evaluationInstructions();
  }

  /**
   * Get the operand variable name.
   */
  public String getOperand() {
    return operandEvaluation.operandName();
  }

  /**
   * Get the null check condition variable name (result of IS_NULL check).
   */
  public String getNullCheckCondition() {
    return nullCheckCondition;
  }

  /**
   * Get instructions for the null case (when operand is null).
   */
  public List<IRInstr> getNullCaseEvaluationInstructions() {
    return nullCaseEvaluation.evaluationInstructions();
  }

  /**
   * Get the result variable for the null case.
   */
  public String getNullResult() {
    return nullCaseEvaluation.operandName();
  }

  /**
   * Get instructions for the set case (when operand is not null).
   */
  public List<IRInstr> getSetCaseEvaluationInstructions() {
    return setCaseEvaluation.evaluationInstructions();
  }

  /**
   * Get the result variable for the set case.
   */
  public String getSetResult() {
    return setCaseEvaluation.operandName();
  }

  /**
   * Get the scope ID for memory management.
   */
  public String getScopeId() {
    return scopeId;
  }

  /**
   * Get the operand evaluation record.
   */
  public OperandEvaluation getOperandEvaluation() {
    return operandEvaluation;
  }

  /**
   * Get the null case evaluation record.
   */
  public OperandEvaluation getNullCaseEvaluation() {
    return nullCaseEvaluation;
  }

  /**
   * Get the set case evaluation record.
   */
  public OperandEvaluation getSetCaseEvaluation() {
    return setCaseEvaluation;
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

    // Operand evaluation section
    sb.append("operand_evaluation:\n[\n");
    if (operandEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : operandEvaluation.evaluationInstructions()) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("operand: ").append(operandEvaluation.operandName()).append("\n");
    sb.append("null_check_condition: ").append(nullCheckCondition).append("\n");

    // Null case evaluation section
    sb.append("null_case_evaluation:\n[\n");
    if (nullCaseEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : nullCaseEvaluation.evaluationInstructions()) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("null_result: ").append(nullCaseEvaluation.operandName()).append("\n");

    // Set case evaluation section
    sb.append("set_case_evaluation:\n[\n");
    if (setCaseEvaluation.evaluationInstructions().isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : setCaseEvaluation.evaluationInstructions()) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("set_result: ").append(setCaseEvaluation.operandName()).append("\n");
    sb.append("scope_id: ").append(scopeId).append("\n");

    sb.append("]");

    return sb.toString();
  }
}