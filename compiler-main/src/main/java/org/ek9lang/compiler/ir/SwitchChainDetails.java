package org.ek9lang.compiler.ir;

import java.util.List;
import org.ek9lang.compiler.phase7.support.BasicDetails;

/**
 * Record containing all information needed to create a SWITCH_CHAIN_BLOCK instruction.
 * <p>
 * This record aggregates all the necessary data for generating the unified control flow IR,
 * supporting all variants: Question operators, if/else statements, switch statements, and
 * guarded assignments.
 * </p>
 * <p>
 * Used by SwitchChainBlockGenerator to coordinate the creation of SWITCH_CHAIN_BLOCK instructions
 * with consistent memory management and optimization hints.
 * </p>
 */
public record SwitchChainDetails(
    /**
     * Result variable name for this control flow block.
     * For expressions: the computed result  
     * For statements: null or void indicator
     */
    String result,

    /**
     * Type of control flow construct for backend optimization.
     * Values: "QUESTION_OPERATOR", "IF_ELSE", "IF_ELSE_IF", "SWITCH", "SWITCH_ENUM"
     */
    String chainType,

    /**
     * Variable being evaluated (for switch statements).
     * null for if/else statements and Question operators.
     * Example: the variable in "switch someVar"
     */
    String evaluationVariable,

    /**
     * Fully qualified type name of the evaluation variable.
     * null if evaluationVariable is null.
     */
    String evaluationVariableType,

    /**
     * Instructions to setup/compute the evaluation variable.
     * Used for switch declarations like "switch var := expr"
     */
    List<IRInstr> evaluationVariableSetup,

    /**
     * Explicit return variable for expression forms.
     * Used when switch/if is an expression with declared return variable.
     * Example: the "result" in "result <- switch expr"
     */
    String returnVariable,

    /**
     * Type of the return variable.
     * null if returnVariable is null.
     */
    String returnVariableType,

    /**
     * Instructions to setup the return variable.
     * Typically REFERENCE declaration and scope registration.
     */
    List<IRInstr> returnVariableSetup,

    /**
     * Sequential list of condition cases to evaluate.
     * Each case contains condition evaluation and body execution instructions.
     */
    List<ConditionCase> conditionChain,

    /**
     * Instructions for the default/else case.
     * Executed when no condition in conditionChain matches.
     */
    List<IRInstr> defaultBodyEvaluation,

    /**
     * Result variable for the default case.
     * null if no default case or default produces no result.
     */
    String defaultResult,

    /**
     * Enum-specific optimization information.
     * null for non-enum switches.
     */
    EnumOptimizationInfo enumOptimizationInfo,

    /**
     * Basic details including scope ID and debug information.
     */
    BasicDetails basicDetails
) {

  /**
   * Create details for a Question operator (?).
   */
  public static SwitchChainDetails createQuestionOperator(
      String result,
      List<ConditionCase> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      BasicDetails basicDetails) {
    
    return new SwitchChainDetails(
        result,
        "QUESTION_OPERATOR",
        null, // No evaluation variable
        null, // No evaluation variable type  
        List.of(), // No evaluation variable setup
        null, // No explicit return variable
        null, // No return variable type
        List.of(), // No return variable setup
        conditionChain,
        defaultBodyEvaluation,
        defaultResult,
        null, // No enum optimization
        basicDetails
    );
  }

  /**
   * Create details for an if/else statement.
   */
  public static SwitchChainDetails createIfElse(
      String result,
      List<ConditionCase> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      BasicDetails basicDetails) {
    
    return new SwitchChainDetails(
        result,
        conditionChain.size() > 1 ? "IF_ELSE_IF" : "IF_ELSE",
        null, // No evaluation variable
        null, // No evaluation variable type
        List.of(), // No evaluation variable setup
        null, // No explicit return variable
        null, // No return variable type
        List.of(), // No return variable setup
        conditionChain,
        defaultBodyEvaluation,
        defaultResult,
        null, // No enum optimization
        basicDetails
    );
  }

  /**
   * Create details for a switch statement with enum optimization.
   */
  public static SwitchChainDetails createSwitchEnum(
      String result,
      String evaluationVariable,
      String evaluationVariableType,
      List<IRInstr> evaluationVariableSetup,
      String returnVariable,
      String returnVariableType,
      List<IRInstr> returnVariableSetup,
      List<ConditionCase> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      EnumOptimizationInfo enumOptimizationInfo,
      BasicDetails basicDetails) {
    
    return new SwitchChainDetails(
        result,
        "SWITCH_ENUM",
        evaluationVariable,
        evaluationVariableType,
        evaluationVariableSetup,
        returnVariable,
        returnVariableType,
        returnVariableSetup,
        conditionChain,
        defaultBodyEvaluation,
        defaultResult,
        enumOptimizationInfo,
        basicDetails
    );
  }

  /**
   * Create details for a general switch statement.
   */
  public static SwitchChainDetails createSwitch(
      String result,
      String evaluationVariable,
      String evaluationVariableType,
      List<IRInstr> evaluationVariableSetup,
      String returnVariable,
      String returnVariableType,
      List<IRInstr> returnVariableSetup,
      List<ConditionCase> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      BasicDetails basicDetails) {
    
    return new SwitchChainDetails(
        result,
        "SWITCH",
        evaluationVariable,
        evaluationVariableType,
        evaluationVariableSetup,
        returnVariable,
        returnVariableType,
        returnVariableSetup,
        conditionChain,
        defaultBodyEvaluation,
        defaultResult,
        null, // No enum optimization for general switch
        basicDetails
    );
  }

  /**
   * Check if this switch has an evaluation variable.
   */
  public boolean hasEvaluationVariable() {
    return evaluationVariable != null;
  }

  /**
   * Check if this switch has a return variable.
   */
  public boolean hasReturnVariable() {
    return returnVariable != null;
  }

  /**
   * Check if this switch has a default case.
   */
  public boolean hasDefaultCase() {
    return defaultBodyEvaluation != null && !defaultBodyEvaluation.isEmpty();
  }

  /**
   * Check if this switch has enum optimization information.
   */
  public boolean hasEnumOptimization() {
    return enumOptimizationInfo != null;
  }

  /**
   * Get the scope ID from basic details.
   */
  public String getScopeId() {
    return basicDetails.scopeId();
  }

  /**
   * Check if this is a Question operator.
   */
  public boolean isQuestionOperator() {
    return "QUESTION_OPERATOR".equals(chainType);
  }

  /**
   * Check if this is an if/else construct.
   */
  public boolean isIfElse() {
    return "IF_ELSE".equals(chainType) || "IF_ELSE_IF".equals(chainType);
  }

  /**
   * Check if this is a switch construct.
   */
  public boolean isSwitch() {
    return "SWITCH".equals(chainType) || "SWITCH_ENUM".equals(chainType);
  }
}