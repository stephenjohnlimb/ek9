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
 * guarded assignments, guard variable patterns, and try/catch/finally exception handling.
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
     *         "FOR_WITH_GUARDS", "GUARDED_ASSIGNMENT", "TRY_CATCH_FINALLY",
     *         legacy types preserved
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
     * Try block information for try/catch/finally constructs.
     * Contains try scope and try body evaluation.
     * null for non-exception-handling constructs.
     */
    TryBlockDetails tryBlockDetails,

    /*
     * Finally block evaluation instructions.
     * Contains instructions that always execute after try/catch.
     * Empty list for constructs without finally blocks.
     */
    List<IRInstr> finallyBlockEvaluation,

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
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a Null Coalescing operator (??).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createNullCoalescing(
      String result,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "NULL_COALESCING_OPERATOR",
        GuardVariableDetails.none(), // No guard variables for null coalescing
        EvaluationVariableDetails.none(), // No evaluation variable for null coalescing
        ReturnVariableDetails.none(), // No return variable for null coalescing
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for an Elvis Coalescing operator (:?).
   * Returns LHS if both memory allocated AND set, otherwise evaluates and returns RHS.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createElvisCoalescing(
      String result,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "ELVIS_COALESCING_OPERATOR",
        GuardVariableDetails.none(), // No guard variables for elvis coalescing
        EvaluationVariableDetails.none(), // No evaluation variable for elvis coalescing
        ReturnVariableDetails.none(), // No return variable for elvis coalescing
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No enum optimization
        null, // No try block
        List.of(), // No finally block
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
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a while loop (statement form).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param conditionChain Single ConditionCaseDetails with loop condition and body
   * @param debugInfo      Debug information
   * @param scopeId        Condition scope ID (_scope_2) where temps are registered
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
        null,                           // No try block
        List.of(),                      // No finally block
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
   * @param debugInfo      Debug information
   * @param scopeId        Whole loop scope ID (_scope_2) for loop control
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
        null,                           // No try block
        List.of(),                      // No finally block
        debugInfo,
        scopeId                         // Whole loop scope ID
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
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a try/catch/finally construct.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   * <p>
   * Try/catch/finally uses CONTROL_FLOW_CHAIN with exception handlers as condition cases.
   * Each catch block is represented as a ConditionCaseDetails with EXCEPTION_HANDLER type.
   * The finally block (if present) executes after try/catch completion.
   * </p>
   *
   * @param result                 Result variable for expression forms (null for statement forms)
   * @param guardDetails           Guard variable details for pre-flow (try var &lt;- getValue())
   * @param returnDetails          Return variable details for expression forms
   * @param tryBlockDetails        Try block scope and body evaluation
   * @param catchHandlers          List of catch handlers (each is a ConditionCaseDetails with EXCEPTION_HANDLER type)
   * @param finallyBlockEvaluation Finally block instructions (empty if no finally)
   * @param debugInfo              Debug information
   * @param scopeId                Outer scope ID for the entire try/catch/finally construct
   * @return ControlFlowChainDetails configured for try/catch/finally
   */
  public static ControlFlowChainDetails createTryCatchFinally(
      String result,
      GuardVariableDetails guardDetails,
      ReturnVariableDetails returnDetails,
      TryBlockDetails tryBlockDetails,
      List<ConditionCaseDetails> catchHandlers,
      List<IRInstr> finallyBlockEvaluation,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "TRY_CATCH_FINALLY",
        guardDetails,
        EvaluationVariableDetails.none(), // No evaluation variable for try/catch
        returnDetails,
        catchHandlers, // Catch blocks represented as condition cases
        DefaultCaseDetails.none(), // No default case (finally is not a default)
        null, // No enum optimization
        tryBlockDetails,
        finallyBlockEvaluation,
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

  /**
   * Check if this construct has a try block.
   */
  public boolean hasTryBlock() {
    return tryBlockDetails != null;
  }

  /**
   * Get try scope ID.
   */
  public String tryScopeId() {
    return tryBlockDetails != null ? tryBlockDetails.tryScopeId() : null;
  }

  /**
   * Get try body evaluation instructions.
   */
  public List<IRInstr> tryBodyEvaluation() {
    return tryBlockDetails != null ? tryBlockDetails.tryBodyEvaluation() : List.of();
  }

  /**
   * Get try body result variable.
   */
  public String tryBodyResult() {
    return tryBlockDetails != null ? tryBlockDetails.tryBodyResult() : null;
  }

  /**
   * Check if this construct has a finally block.
   */
  public boolean hasFinallyBlock() {
    return finallyBlockEvaluation != null && !finallyBlockEvaluation.isEmpty();
  }
}