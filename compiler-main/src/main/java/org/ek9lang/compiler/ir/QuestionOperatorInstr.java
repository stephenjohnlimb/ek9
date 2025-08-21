package org.ek9lang.compiler.ir;

import java.util.List;
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

  private final List<IRInstr> operandEvaluationInstructions;
  private final String operand;
  private final String nullCheckCondition;
  private final List<IRInstr> nullCaseEvaluationInstructions;
  private final String nullResult;
  private final List<IRInstr> setCaseEvaluationInstructions;
  private final String setResult;
  private final String scopeId;

  /**
   * Create question operator block.
   */
  public static QuestionOperatorInstr questionBlock(final String result,
                                                   final List<IRInstr> operandEvaluationInstructions,
                                                   final String operand,
                                                   final String nullCheckCondition,
                                                   final List<IRInstr> nullCaseEvaluationInstructions,
                                                   final String nullResult,
                                                   final List<IRInstr> setCaseEvaluationInstructions,
                                                   final String setResult,
                                                   final String scopeId,
                                                   final DebugInfo debugInfo) {
    return new QuestionOperatorInstr(result, operandEvaluationInstructions, operand, nullCheckCondition,
        nullCaseEvaluationInstructions, nullResult, setCaseEvaluationInstructions, setResult,
        scopeId, debugInfo);
  }

  private QuestionOperatorInstr(final String result,
                               final List<IRInstr> operandEvaluationInstructions,
                               final String operand,
                               final String nullCheckCondition,
                               final List<IRInstr> nullCaseEvaluationInstructions,
                               final String nullResult,
                               final List<IRInstr> setCaseEvaluationInstructions,
                               final String setResult,
                               final String scopeId,
                               final DebugInfo debugInfo) {
    super(IROpcode.QUESTION_BLOCK, result, debugInfo);

    AssertValue.checkNotNull("Operand evaluation instructions cannot be null", operandEvaluationInstructions);
    AssertValue.checkNotNull("Operand cannot be null", operand);
    AssertValue.checkNotNull("Null check condition cannot be null", nullCheckCondition);
    AssertValue.checkNotNull("Null case evaluation instructions cannot be null", nullCaseEvaluationInstructions);
    AssertValue.checkNotNull("Null result cannot be null", nullResult);
    AssertValue.checkNotNull("Set case evaluation instructions cannot be null", setCaseEvaluationInstructions);
    AssertValue.checkNotNull("Set result cannot be null", setResult);
    AssertValue.checkNotNull("Scope ID cannot be null", scopeId);

    this.operandEvaluationInstructions = operandEvaluationInstructions;
    this.operand = operand;
    this.nullCheckCondition = nullCheckCondition;
    this.nullCaseEvaluationInstructions = nullCaseEvaluationInstructions;
    this.nullResult = nullResult;
    this.setCaseEvaluationInstructions = setCaseEvaluationInstructions;
    this.setResult = setResult;
    this.scopeId = scopeId;
  }

  /**
   * Get instructions for evaluating the operand (the expression that ? is applied to).
   */
  public List<IRInstr> getOperandEvaluationInstructions() {
    return operandEvaluationInstructions;
  }

  /**
   * Get the operand variable name.
   */
  public String getOperand() {
    return operand;
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
    return nullCaseEvaluationInstructions;
  }

  /**
   * Get the result variable for the null case.
   */
  public String getNullResult() {
    return nullResult;
  }

  /**
   * Get instructions for the set case (when operand is not null).
   */
  public List<IRInstr> getSetCaseEvaluationInstructions() {
    return setCaseEvaluationInstructions;
  }

  /**
   * Get the result variable for the set case.
   */
  public String getSetResult() {
    return setResult;
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

    // Operand evaluation section
    sb.append("operand_evaluation:\n[\n");
    if (operandEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : operandEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("operand: ").append(operand).append("\n");
    sb.append("null_check_condition: ").append(nullCheckCondition).append("\n");

    // Null case evaluation section
    sb.append("null_case_evaluation:\n[\n");
    if (nullCaseEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : nullCaseEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("null_result: ").append(nullResult).append("\n");

    // Set case evaluation section
    sb.append("set_case_evaluation:\n[\n");
    if (setCaseEvaluationInstructions.isEmpty()) {
      sb.append("no instructions\n");
    } else {
      for (IRInstr instr : setCaseEvaluationInstructions) {
        String instrStr = instr.toString();
        sb.append(instrStr);
        // Add newline if instruction doesn't already end with one
        if (!instrStr.endsWith("\n")) {
          sb.append("\n");
        }
      }
    }
    sb.append("]\n");
    sb.append("set_result: ").append(setResult).append("\n");
    sb.append("scope_id: ").append(scopeId).append("\n");

    sb.append("]");

    return sb.toString();
  }
}