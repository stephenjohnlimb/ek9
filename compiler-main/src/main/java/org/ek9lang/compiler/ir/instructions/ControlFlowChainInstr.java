package org.ek9lang.compiler.ir.instructions;

import java.util.List;
import org.ek9lang.compiler.ir.IROpcode;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.EnumOptimizationDetails;
import org.ek9lang.compiler.ir.data.TryBlockDetails;
import org.ek9lang.core.AssertValue;

/**
 * Unified IR instruction for all EK9 control flow constructs.
 * <p>
 * This instruction replaces and unifies the following specialized IR opcodes:
 * - QUESTION_BLOCK (Question operator ?)
 * - GUARDED_ASSIGNMENT_BLOCK (Guarded assignment :=?)
 * - Future: IF_BLOCK, SWITCH_BLOCK constructs
 * </p>
 * <p>
 * Key architectural benefits:
 * - Single source of truth for all control flow logic
 * - Consistent memory management across all constructs
 * - Enhanced backend optimization opportunities
 * - Reduced IR complexity and maintenance burden
 * </p>
 * <p>
 * Backends can optimize based on chainType hints and optimization metadata:
 * - QUESTION_OPERATOR: Null check + method call optimization
 * - IF_ELSE/IF_ELSE_IF: Standard conditional branching
 * - SWITCH/SWITCH_ENUM: Jump tables, binary search, or sequential evaluation
 * </p>
 */
public final class ControlFlowChainInstr extends IRInstr {

  private final String chainType;
  private final String evaluationVariable;
  private final String evaluationVariableType;
  private final List<IRInstr> evaluationVariableSetup;
  private final String returnVariable;
  private final String returnVariableType;
  private final List<IRInstr> returnVariableSetup;
  private final List<ConditionCaseDetails> conditionChain;
  private final List<IRInstr> defaultBodyEvaluation;
  private final String defaultResult;
  private final EnumOptimizationDetails enumOptimizationInfo;
  private final TryBlockDetails tryBlockDetails;
  private final List<IRInstr> finallyBlockEvaluation;
  private final String scopeId;

  /**
   * Create a unified switch chain block instruction.
   */
  public static ControlFlowChainInstr controlFlowChain(final ControlFlowChainDetails details) {
    return new ControlFlowChainInstr(details);
  }

  private ControlFlowChainInstr(final ControlFlowChainDetails details) {
    super(IROpcode.CONTROL_FLOW_CHAIN, details.result(), details.debugInfo());

    AssertValue.checkNotNull("ControlFlowChain details cannot be null", details);
    AssertValue.checkNotNull("Chain type cannot be null", details.chainType());
    AssertValue.checkNotNull("Condition chain cannot be null", details.conditionChain());

    this.chainType = details.chainType();
    this.evaluationVariable = details.evaluationVariable();
    this.evaluationVariableType = details.evaluationVariableType();
    this.evaluationVariableSetup = details.evaluationVariableSetup() != null
        ? details.evaluationVariableSetup() : List.of();
    this.returnVariable = details.returnVariable();
    this.returnVariableType = details.returnVariableType();
    this.returnVariableSetup = details.returnVariableSetup() != null
        ? details.returnVariableSetup() : List.of();
    this.conditionChain = details.conditionChain();
    this.defaultBodyEvaluation = details.defaultBodyEvaluation() != null
        ? details.defaultBodyEvaluation() : List.of();
    this.defaultResult = details.defaultResult();
    this.enumOptimizationInfo = details.enumOptimizationInfo();
    this.tryBlockDetails = details.tryBlockDetails();
    this.finallyBlockEvaluation = details.finallyBlockEvaluation() != null
        ? details.finallyBlockEvaluation() : List.of();
    this.scopeId = details.scopeId();

    // Add operands for base class functionality
    addOperand(chainType);
    addOperand(scopeId);
    if (evaluationVariable != null) {
      addOperand(evaluationVariable);
    }
    if (returnVariable != null) {
      addOperand(returnVariable);
    }
  }

  /**
   * Get the type of control flow construct.
   */
  public String getChainType() {
    return chainType;
  }

  /**
   * Get the sequential condition chain.
   */
  public List<ConditionCaseDetails> getConditionChain() {
    return conditionChain;
  }

  /**
   * Get the default/else case evaluation instructions.
   */
  public List<IRInstr> getDefaultBodyEvaluation() {
    return defaultBodyEvaluation;
  }

  /**
   * Get the default case result variable.
   */
  public String getDefaultResult() {
    return defaultResult;
  }

  /**
   * Get the scope ID for memory management.
   */
  public String getScopeId() {
    return scopeId;
  }

  /**
   * Get the try block details (for TRY_CATCH_FINALLY).
   */
  public TryBlockDetails getTryBlockDetails() {
    return tryBlockDetails;
  }

  /**
   * Check if this construct has an evaluation variable.
   */
  public boolean hasEvaluationVariable() {
    return evaluationVariable != null;
  }

  /**
   * Check if this construct has a return variable.
   */
  public boolean hasReturnVariable() {
    return returnVariable != null;
  }

  /**
   * Check if this construct has a default case.
   */
  public boolean hasDefaultCase() {
    return !defaultBodyEvaluation.isEmpty();
  }

  /**
   * Check if this construct has enum optimization information.
   */
  public boolean hasEnumOptimization() {
    return enumOptimizationInfo != null;
  }

  /**
   * Check if this is a switch construct.
   */
  public boolean isSwitch() {
    return "SWITCH".equals(chainType) || "SWITCH_ENUM".equals(chainType);
  }

  @Override
  public String toString() {
    var builder = new StringBuilder();

    appendInstructionHeader(builder);
    builder.append("\n[\n");
    builder.append("chain_type: \"").append(chainType).append("\"\n");

    appendEvaluationVariableSection(builder);
    appendReturnVariableSection(builder);
    appendTryBlockSection(builder);
    appendConditionChainSection(builder);
    appendFinallyBlockSection(builder);
    appendDefaultCaseSection(builder);
    appendEnumOptimizationSection(builder);

    builder.append("scope_id: ").append(scopeId).append("\n");
    builder.append("]");

    return builder.toString();
  }

  private void appendInstructionHeader(StringBuilder builder) {
    if (getResult() != null) {
      builder.append(getResult()).append(" = ");
    }
    builder.append(getOpcode().name());
    if (getDebugInfo().isPresent() && getDebugInfo().get().isValidLocation()) {
      builder.append("  ").append(getDebugInfo().get());
    }
  }

  private void appendEvaluationVariableSection(StringBuilder builder) {
    if (hasEvaluationVariable()) {
      builder.append("evaluation_variable: ").append(evaluationVariable).append("\n");
      builder.append("evaluation_variable_type: \"").append(evaluationVariableType).append("\"\n");

      if (!evaluationVariableSetup.isEmpty()) {
        appendInstructionList(builder, "evaluation_variable_setup", evaluationVariableSetup);
      }
    }
  }

  private void appendReturnVariableSection(StringBuilder builder) {
    if (hasReturnVariable()) {
      builder.append("return_variable: ").append(returnVariable).append("\n");
      builder.append("return_variable_type: \"").append(returnVariableType).append("\"\n");

      if (!returnVariableSetup.isEmpty()) {
        appendInstructionList(builder, "return_variable_setup", returnVariableSetup);
      }
    }
  }

  private void appendTryBlockSection(StringBuilder builder) {
    if (tryBlockDetails != null) {
      builder.append("try_block_details:\n[\n");

      if (tryBlockDetails.tryScopeId() != null) {
        builder.append("try_scope_id: ").append(tryBlockDetails.tryScopeId()).append("\n");
      }

      if (!tryBlockDetails.tryBodyEvaluation().isEmpty()) {
        appendInstructionList(builder, "try_body_evaluation", tryBlockDetails.tryBodyEvaluation());
      }

      if (tryBlockDetails.tryBodyResult() != null) {
        builder.append("try_body_result: ").append(tryBlockDetails.tryBodyResult()).append("\n");
      }

      builder.append("]\n");
    }
  }

  private void appendConditionChainSection(StringBuilder builder) {
    builder.append("condition_chain:\n[\n");
    for (int i = 0; i < conditionChain.size(); i++) {
      appendConditionCase(builder, conditionChain.get(i));
      if (i < conditionChain.size() - 1) {
        builder.append(",");
      }
      builder.append("\n");
    }
    builder.append("]\n");
  }

  private void appendFinallyBlockSection(StringBuilder builder) {
    if (!finallyBlockEvaluation.isEmpty()) {
      appendInstructionList(builder, "finally_block_evaluation", finallyBlockEvaluation);
    }
  }

  private void appendConditionCase(StringBuilder builder, ConditionCaseDetails conditionCase) {
    builder.append("[\n");

    if (conditionCase.caseScopeId() != null) {
      builder.append("case_scope_id: ").append(conditionCase.caseScopeId()).append("\n");
    }

    builder.append("case_type: \"").append(conditionCase.caseType()).append("\"\n");

    if (conditionCase.enumConstant() != null) {
      builder.append("enum_constant: \"").append(conditionCase.enumConstant()).append("\"\n");
      builder.append("enum_ordinal: ").append(conditionCase.enumOrdinal()).append("\n");
    }

    appendInstructionList(builder, "condition_evaluation", conditionCase.conditionEvaluation());

    if (conditionCase.conditionResult() != null) {
      builder.append("condition_result: ").append(conditionCase.conditionResult()).append("\n");
    }

    if (conditionCase.primitiveCondition() != null) {
      builder.append("primitive_condition: ").append(conditionCase.primitiveCondition()).append("\n");
    }

    appendInstructionList(builder, "body_evaluation", conditionCase.bodyEvaluation());

    if (conditionCase.bodyResult() != null) {
      builder.append("body_result: ").append(conditionCase.bodyResult()).append("\n");
    }

    if (conditionCase.exceptionType() != null) {
      builder.append("exception_type: \"").append(conditionCase.exceptionType()).append("\"\n");
    }

    if (conditionCase.exceptionVariable() != null) {
      builder.append("exception_variable: ").append(conditionCase.exceptionVariable()).append("\n");
    }

    builder.append("]");
  }

  private void appendDefaultCaseSection(StringBuilder builder) {
    if (hasDefaultCase()) {
      appendInstructionList(builder, "default_body_evaluation", defaultBodyEvaluation);

      if (defaultResult != null) {
        builder.append("default_result: ").append(defaultResult).append("\n");
      }
    }
  }

  private void appendEnumOptimizationSection(StringBuilder builder) {
    if (hasEnumOptimization()) {
      builder.append("enum_optimization_info:\n[\n");
      builder.append("enum_type: \"").append(enumOptimizationInfo.enumType()).append("\"\n");
      builder.append("enum_values: ").append(enumOptimizationInfo.enumValues()).append("\n");
      builder.append("enum_ordinals: ").append(enumOptimizationInfo.enumOrdinals()).append("\n");
      builder.append("is_exhaustive: ").append(enumOptimizationInfo.isExhaustive()).append("\n");
      builder.append("is_dense: ").append(enumOptimizationInfo.isDense()).append("\n");
      builder.append("]\n");
    }
  }

  private void appendInstructionList(StringBuilder builder, String label, List<IRInstr> instructions) {
    builder.append(label).append(":\n[\n");
    for (IRInstr instr : instructions) {
      builder.append(instr.toString()).append("\n");
    }
    builder.append("]\n");
  }
}