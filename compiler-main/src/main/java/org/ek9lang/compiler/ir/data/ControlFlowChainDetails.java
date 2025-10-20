package org.ek9lang.compiler.ir.data;

import java.util.List;
import javax.annotation.Nonnull;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Record containing all information needed to create a CONTROL_FLOW_CHAIN instruction.
 * <p>
 * This record aggregates all the necessary data for generating the unified control flow IR,
 * supporting all variants: Question operators, if/else statements, switch statements,
 * guarded assignments, and guard variable patterns.
 * </p>
 * <p>
 * Used by ControlFlowChainGenerator to coordinate the creation of CONTROL_FLOW_CHAIN instructions
 * with consistent memory management, scope boundaries, and optimization hints.
 * </p>
 */
public record ControlFlowChainDetails(
    /*
     * Result variable name for this control flow block.
     * For expressions: the computed result  
     * For statements: null or void indicator
     */
    String result,

    /*
     * Type of control flow construct for backend optimization.
     * Values: "QUESTION_OPERATOR", "IF_ELSE_WITH_GUARDS", "SWITCH_WITH_GUARDS", 
     *         "FOR_WITH_GUARDS", "GUARDED_ASSIGNMENT", legacy types preserved
     */
    String chainType,

    /*
     * Guard variable management details.
     * Contains guard variables, scope setup, and scope IDs for guard-enabled constructs.
     * Empty/null for constructs without guard variables.
     */
    GuardVariableDetails guardDetails,

    /*
     * Evaluation variable management details.
     * Contains evaluation variable, type, and setup for switch statements.
     * Empty/null for if/else statements and Question operators.
     */
    EvaluationVariableDetails evaluationDetails,

    /*
     * Return variable management details.
     * Contains return variable, type, and setup for expression forms.
     * Empty/null for statement forms.
     */
    ReturnVariableDetails returnDetails,

    /*
     * Sequential list of condition cases to evaluate.
     * Each case contains condition evaluation and body execution instructions.
     */
    List<ConditionCaseDetails> conditionChain,

    /*
     * Default case management details.
     * Contains default body evaluation and result for fallback behavior.
     * Empty/null if no default case.
     */
    DefaultCaseDetails defaultDetails,

    /*
     * Enum-specific optimization information.
     * null for non-enum switches.
     */
    EnumOptimizationDetails enumOptimizationInfo,

    /*
     * Debug information for this control flow construct.
     * STACK-BASED: debugInfo extracted directly at Details creation time.
     */
    DebugInfo debugInfo,

    /*
     * Scope ID extracted from stack context at Details creation time.
     * STACK-BASED: All scope information comes from IRGenerationContext.currentScopeId().
     */
    String scopeId
) {

  /**
   * Create details for a Question operator (?).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createQuestionOperator(
      String result,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {
    
    return new ControlFlowChainDetails(
        result,
        "QUESTION_OPERATOR",
        GuardVariableDetails.none(), // No guard variables for question operator
        EvaluationVariableDetails.none(), // No evaluation variable for question operator
        ReturnVariableDetails.none(), // No return variable for question operator
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for an if/else statement.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createIfElse(
      String result,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "IF_ELSE_IF",
        GuardVariableDetails.none(), // No guard variables for legacy if/else
        EvaluationVariableDetails.none(), // No evaluation variable for if/else
        ReturnVariableDetails.none(), // No return variable for legacy if/else
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a while loop (statement form).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param conditionChain Single ConditionCaseDetails with loop condition and body
   * @param debugInfo Debug information
   * @param scopeId Condition scope ID (_scope_2) where temps are registered
   * @return ControlFlowChainDetails configured for while loop
   */
  public static ControlFlowChainDetails createWhileLoop(
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        null,                           // No result for statement form
        "WHILE_LOOP",                   // Chain type for backend
        GuardVariableDetails.none(),    // No guards yet
        EvaluationVariableDetails.none(), // No evaluation variable
        ReturnVariableDetails.none(),   // No return variable (statement form)
        conditionChain,                 // Single case with condition + body
        DefaultCaseDetails.none(),      // No default case
        null,                           // No enum optimization
        debugInfo,
        scopeId                         // Condition scope ID
    );
  }

  /**
   * Create details for a do-while loop (statement form).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   * <p>
   * Key difference from while loop: Body executes FIRST (at least once),
   * then condition is evaluated. Backend generates IFNE (jump if true)
   * instead of IFEQ (jump if false).
   * </p>
   *
   * @param conditionChain Single ConditionCaseDetails with loop body and condition
   * @param debugInfo Debug information
   * @param scopeId Whole loop scope ID (_scope_2) for loop control
   * @return ControlFlowChainDetails configured for do-while loop
   */
  public static ControlFlowChainDetails createDoWhileLoop(
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        null,                           // No result for statement form
        "DO_WHILE_LOOP",                // Chain type for backend
        GuardVariableDetails.none(),    // No guards yet
        EvaluationVariableDetails.none(), // No evaluation variable
        ReturnVariableDetails.none(),   // No return variable (statement form)
        conditionChain,                 // Single case with body + condition
        DefaultCaseDetails.none(),      // No default case
        null,                           // No enum optimization
        debugInfo,
        scopeId                         // Whole loop scope ID
    );
  }

  /**
   * Create details for a for-range loop (statement form).
   * For-range loops are transformed into while loops with range state setup.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param conditionChain Single ConditionCaseDetails with loop condition and body
   * @param debugInfo Debug information
   * @param scopeId Loop control scope ID (_scope_3) for loop header
   * @return ControlFlowChainDetails configured for for-range loop
   */
  public static ControlFlowChainDetails createForRangeLoop(
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        null,                           // No result for statement form
        "FOR_RANGE_LOOP",               // Chain type for backend
        GuardVariableDetails.none(),    // No guards yet
        EvaluationVariableDetails.none(), // No evaluation variable
        ReturnVariableDetails.none(),   // No return variable (statement form)
        conditionChain,                 // Single case with condition + body
        DefaultCaseDetails.none(),      // No default case
        null,                           // No enum optimization
        debugInfo,
        scopeId                         // Loop control scope ID
    );
  }

  /**
   * Create details for a switch statement with enum optimization.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createSwitchEnum(
      String result,
      String evaluationVariable,
      String evaluationVariableType,
      List<IRInstr> evaluationVariableSetup,
      String returnVariable,
      String returnVariableType,
      List<IRInstr> returnVariableSetup,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      EnumOptimizationDetails enumOptimizationInfo,
      DebugInfo debugInfo,
      String scopeId) {
    
    return new ControlFlowChainDetails(
        result,
        "SWITCH_ENUM",
        GuardVariableDetails.none(), // No guard variables in legacy enum switch
        EvaluationVariableDetails.withSetup(evaluationVariable, evaluationVariableType, evaluationVariableSetup),
        ReturnVariableDetails.withSetup(returnVariable, returnVariableType, returnVariableSetup),
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        enumOptimizationInfo,
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a general switch statement.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createSwitch(
      String result,
      String evaluationVariable,
      String evaluationVariableType,
      List<IRInstr> evaluationVariableSetup,
      String returnVariable,
      String returnVariableType,
      List<IRInstr> returnVariableSetup,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {
    
    return new ControlFlowChainDetails(
        result,
        "SWITCH",
        GuardVariableDetails.none(), // No guard variables in legacy switch
        EvaluationVariableDetails.withSetup(evaluationVariable, evaluationVariableType, evaluationVariableSetup),
        ReturnVariableDetails.withSetup(returnVariable, returnVariableType, returnVariableSetup),
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization for general switch
        debugInfo,
        scopeId
    );
  }

  /**
   * Check if this switch has enum optimization information.
   */
  public boolean hasEnumOptimization() {
    return enumOptimizationInfo != null;
  }

  /**
   * Get the scope ID extracted from stack context at Details creation time.
   * STACK-BASED: scopeId comes from IRGenerationContext.currentScopeId() where this object is created.
   */
  public String getScopeId() {
    return scopeId;
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
    return "IF_ELSE_IF".equals(chainType);
  }

  /**
   * Check if this is a switch construct.
   */
  public boolean isSwitch() {
    return "SWITCH".equals(chainType) || "SWITCH_ENUM".equals(chainType);
  }

  /**
   * IR-optimized toString following EK9's bracket-only, no-indentation format.
   * Uses StringBuilder for performance and delegates to Details records for their sections.
   */
  @Override
  @Nonnull
  public String toString() {
    var builder = new StringBuilder("ControlFlowChainDetails[");
    builder.append("result=").append(result);
    builder.append(", chainType=").append(chainType);

    // Add guard details if present (handles own conditional rendering)
    if (guardDetails != null && !guardDetails.isEmpty()) {
      builder.append(", guard=").append(guardDetails);
    }

    // Add evaluation details if present
    if (evaluationDetails != null && !evaluationDetails.isEmpty()) {
      builder.append(", eval=").append(evaluationDetails);
    }

    // Add return details if present
    if (returnDetails != null && !returnDetails.isEmpty()) {
      builder.append(", return=").append(returnDetails);
    }

    // Add condition chain details
    builder.append(", conditions=").append(conditionChain);

    // Add default details if present
    if (defaultDetails != null && !defaultDetails.isEmpty()) {
      builder.append(", default=").append(defaultDetails);
    }

    // Add enum optimization if present
    if (enumOptimizationInfo != null) {
      builder.append(", enumOpt=true");
    }

    return builder.append("]").toString();
  }

  /**
   * Create details for if/else with guard variables.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createIfElseWithGuards(
      String result,
      List<String> guardVariables,
      List<IRInstr> guardScopeSetup,
      String guardScopeId,
      String conditionScopeId,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {
    
    return new ControlFlowChainDetails(
        result,
        "IF_ELSE_WITH_GUARDS",
        GuardVariableDetails.create(guardVariables, guardScopeSetup, guardScopeId, conditionScopeId),
        EvaluationVariableDetails.none(), // No evaluation variable for if/else
        ReturnVariableDetails.none(), // No return variable for statement form
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for switch with guard variables.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createSwitchWithGuards(
      String result,
      List<String> guardVariables,
      List<IRInstr> guardScopeSetup,
      String guardScopeId,
      String conditionScopeId,
      String evaluationVariable,
      String evaluationVariableType,
      List<IRInstr> evaluationVariableSetup,
      String returnVariable,
      String returnVariableType,
      List<IRInstr> returnVariableSetup,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {
    
    return new ControlFlowChainDetails(
        result,
        "SWITCH_WITH_GUARDS",
        GuardVariableDetails.create(guardVariables, guardScopeSetup, guardScopeId, conditionScopeId),
        EvaluationVariableDetails.withSetup(evaluationVariable, evaluationVariableType, evaluationVariableSetup),
        ReturnVariableDetails.withSetup(returnVariable, returnVariableType, returnVariableSetup),
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization for guard switches
        debugInfo,
        scopeId
    );
  }

  // Delegation methods for backward compatibility

  /**
   * Get guard variables list.
   */
  public List<String> guardVariables() {
    return guardDetails != null ? guardDetails.guardVariables() : List.of();
  }

  /**
   * Get guard scope setup instructions.
   */
  public List<IRInstr> guardScopeSetup() {
    return guardDetails != null ? guardDetails.guardScopeSetup() : List.of();
  }

  /**
   * Get guard scope ID.
   */
  public String guardScopeId() {
    return guardDetails != null ? guardDetails.guardScopeId() : null;
  }

  /**
   * Get condition scope ID.
   */
  public String conditionScopeId() {
    return guardDetails != null ? guardDetails.conditionScopeId() : null;
  }

  /**
   * Get evaluation variable.
   */
  public String evaluationVariable() {
    return evaluationDetails != null ? evaluationDetails.evaluationVariable() : null;
  }

  /**
   * Get evaluation variable type.
   */
  public String evaluationVariableType() {
    return evaluationDetails != null ? evaluationDetails.evaluationVariableType() : null;
  }

  /**
   * Get evaluation variable setup instructions.
   */
  public List<IRInstr> evaluationVariableSetup() {
    return evaluationDetails != null ? evaluationDetails.evaluationVariableSetup() : List.of();
  }

  /**
   * Get return variable.
   */
  public String returnVariable() {
    return returnDetails != null ? returnDetails.returnVariable() : null;
  }

  /**
   * Get return variable type.
   */
  public String returnVariableType() {
    return returnDetails != null ? returnDetails.returnVariableType() : null;
  }

  /**
   * Get return variable setup instructions.
   */
  public List<IRInstr> returnVariableSetup() {
    return returnDetails != null ? returnDetails.returnVariableSetup() : List.of();
  }

  /**
   * Get default body evaluation instructions.
   */
  public List<IRInstr> defaultBodyEvaluation() {
    return defaultDetails != null ? defaultDetails.defaultBodyEvaluation() : List.of();
  }

  /**
   * Get default result variable.
   */
  public String defaultResult() {
    return defaultDetails != null ? defaultDetails.defaultResult() : null;
  }

  /**
   * Check if this construct uses guard variables.
   */
  public boolean hasGuardVariables() {
    return guardDetails != null && guardDetails.hasGuardVariables();
  }

  /**
   * Check if this construct has a guard scope.
   */
  public boolean hasGuardScope() {
    return guardDetails != null && guardDetails.hasGuardScope();
  }

  /**
   * Check if this construct has a shared condition scope.
   */
  public boolean hasSharedConditionScope() {
    return guardDetails != null && guardDetails.hasSharedConditionScope();
  }

  /**
   * Check if this construct has an evaluation variable.
   */
  public boolean hasEvaluationVariable() {
    return evaluationDetails != null && evaluationDetails.hasEvaluationVariable();
  }

  /**
   * Check if this construct has a return variable.
   */
  public boolean hasReturnVariable() {
    return returnDetails != null && returnDetails.hasReturnVariable();
  }

  /**
   * Check if this construct has a default case.
   */
  public boolean hasDefaultCase() {
    return defaultDetails != null && defaultDetails.hasDefaultCase();
  }

  /**
   * Check if this is a guard-enabled construct.
   */
  public boolean isGuardEnabled() {
    return chainType.endsWith("_WITH_GUARDS");
  }
}