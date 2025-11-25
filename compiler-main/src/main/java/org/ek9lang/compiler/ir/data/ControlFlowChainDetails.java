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
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for comparison coalescing operators (&lt;?, &gt;?, &lt;=?, &gt;=?).
   * Returns LHS if both operands valid and comparison true, else gracefully handles null/unset.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createComparisonCoalescing(
      String result,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId,
      String chainType) {

    return new ControlFlowChainDetails(
        result,
        chainType, // e.g., "LESS_THAN_COALESCING_OPERATOR"
        GuardVariableDetails.none(), // No guard variables for comparison coalescing
        EvaluationVariableDetails.none(), // No evaluation variable for comparison coalescing
        ReturnVariableDetails.none(), // No return variable for comparison coalescing
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a Ternary operator (condition &lt;- thenValue : elseValue).
   * Returns thenValue if condition is true, else returns elseValue.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createTernaryOperator(
      String result,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "TERNARY_OPERATOR",
        GuardVariableDetails.none(), // No guard variables for ternary operator
        EvaluationVariableDetails.none(), // No evaluation variable for ternary operator
        ReturnVariableDetails.none(), // No return variable for ternary operator
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
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
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for an if/else statement with guard variables.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   */
  public static ControlFlowChainDetails createIfElseWithGuards(
      String result,
      GuardVariableDetails guardDetails,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        guardDetails.hasGuardVariables() ? "IF_ELSE_WITH_GUARDS" : "IF_ELSE_IF",
        guardDetails,
        EvaluationVariableDetails.none(), // No evaluation variable for if/else
        ReturnVariableDetails.none(), // No return variable for statement form
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
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
        null,                           // No try block
        List.of(),                      // No finally block
        debugInfo,
        scopeId                         // Condition scope ID
    );
  }

  /**
   * Create details for a while loop with guard variables (statement form).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param guardDetails   Guard variable details (guard &lt;- expr())
   * @param conditionChain Single ConditionCaseDetails with loop condition and body
   * @param debugInfo      Debug information
   * @param scopeId        Condition scope ID (_scope_2) where temps are registered
   * @return ControlFlowChainDetails configured for while loop with guards
   */
  public static ControlFlowChainDetails createWhileLoopWithGuards(
      GuardVariableDetails guardDetails,
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        null,                           // No result for statement form
        guardDetails.hasGuardVariables() ? "WHILE_LOOP_WITH_GUARDS" : "WHILE_LOOP",
        guardDetails,                   // Pass through guard details
        EvaluationVariableDetails.none(), // No evaluation variable
        ReturnVariableDetails.none(),   // No return variable (statement form)
        conditionChain,                 // Single case with condition + body
        DefaultCaseDetails.none(),      // No default case
        null,                           // No try block
        List.of(),                      // No finally block
        debugInfo,
        scopeId                         // Condition scope ID
    );
  }

  /**
   * Create details for a do-while loop with guard variables (statement form).
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   * <p>
   * Key difference from while loop: Body executes FIRST (at least once),
   * then condition is evaluated. Backend generates IFNE (jump if true)
   * instead of IFEQ (jump if false).
   * </p>
   *
   * @param guardDetails   Guard variable details (do guard &lt;- expr() { body } while condition)
   * @param conditionChain Single ConditionCaseDetails with loop body and condition
   * @param debugInfo      Debug information
   * @param scopeId        Whole loop scope ID (_scope_2) for loop control
   * @return ControlFlowChainDetails configured for do-while loop with guards
   */
  public static ControlFlowChainDetails createDoWhileLoopWithGuards(
      GuardVariableDetails guardDetails,
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        null,                           // No result for statement form
        guardDetails.hasGuardVariables() ? "DO_WHILE_LOOP_WITH_GUARDS" : "DO_WHILE_LOOP",
        guardDetails,                   // Pass through guard details
        EvaluationVariableDetails.none(), // No evaluation variable
        ReturnVariableDetails.none(),   // No return variable (statement form)
        conditionChain,                 // Single case with body + condition
        DefaultCaseDetails.none(),      // No default case
        null,                           // No try block
        List.of(),                      // No finally block
        debugInfo,
        scopeId                         // Whole loop scope ID
    );
  }

  /**
   * Create details for a switch statement with guard variable support.
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   * <p>
   * Uses guard details to determine chain type:
   * - SWITCH_WITH_GUARDS when guards are present
   * - SWITCH when no guards present
   * </p>
   */
  public static ControlFlowChainDetails createSwitchWithGuards(
      String result,
      GuardVariableDetails guardDetails,
      String evaluationVariable,
      String evaluationVariableType,
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
        guardDetails.hasGuardVariables() ? "SWITCH_WITH_GUARDS" : "SWITCH",
        guardDetails,
        EvaluationVariableDetails.withSetup(evaluationVariable, evaluationVariableType, List.of()),
        ReturnVariableDetails.withSetup(returnVariable, returnVariableType, returnVariableSetup),
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
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
        tryBlockDetails,
        finallyBlockEvaluation,
        debugInfo,
        scopeId
    );
  }

  // ================================================================================
  // Expression Form Factory Methods
  // These factory methods are for expression forms (result <- control_flow_expr)
  // where the construct produces a value assigned to a declared return variable.
  // Expression forms do NOT support guards (GUARD_USED_IN_EXPRESSION error).
  // ================================================================================

  /**
   * Create details for a switch expression (result &lt;- switch expr).
   * <p>
   * Expression forms return a value via the return variable declared in returningParam.
   * Guards are NOT supported in expression forms (enforced by semantic analysis).
   * </p>
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param result                 The overall result variable for the control flow
   * @param evaluationDetails      Evaluation variable details for the switch expression
   * @param returnDetails          Return variable details from returningParam processing
   * @param conditionChain         List of case conditions with their bodies
   * @param defaultBodyEvaluation  Default case body instructions
   * @param defaultResult          Default case result variable
   * @param debugInfo              Debug information
   * @param scopeId                Scope ID from stack context
   * @return ControlFlowChainDetails configured for switch expression
   */
  public static ControlFlowChainDetails createSwitchExpression(
      String result,
      EvaluationVariableDetails evaluationDetails,
      ReturnVariableDetails returnDetails,
      List<ConditionCaseDetails> conditionChain,
      List<IRInstr> defaultBodyEvaluation,
      String defaultResult,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "SWITCH_EXPRESSION",
        GuardVariableDetails.none(), // Guards NOT supported in expression forms
        evaluationDetails,
        returnDetails,
        conditionChain,
        DefaultCaseDetails.withResult(defaultBodyEvaluation, defaultResult),
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a for-range expression (result &lt;- for i in start ..&lt; end).
   * <p>
   * Expression forms return a value via the return variable declared in returningParam.
   * Guards are NOT supported in expression forms (enforced by semantic analysis).
   * The loop accumulates results into the return variable.
   * </p>
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param result           The overall result variable for the control flow
   * @param returnDetails    Return variable details from returningParam processing
   * @param conditionChain   Loop condition with body (single element)
   * @param debugInfo        Debug information
   * @param scopeId          Scope ID from stack context
   * @return ControlFlowChainDetails configured for for-range expression
   */
  public static ControlFlowChainDetails createForRangeExpression(
      String result,
      ReturnVariableDetails returnDetails,
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "FOR_RANGE_EXPRESSION",
        GuardVariableDetails.none(), // Guards NOT supported in expression forms
        EvaluationVariableDetails.none(), // No evaluation variable for for-range
        returnDetails,
        conditionChain,
        DefaultCaseDetails.none(), // No default case for loops
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a for-in expression (result &lt;- for item in collection).
   * <p>
   * Expression forms return a value via the return variable declared in returningParam.
   * Guards are NOT supported in expression forms (enforced by semantic analysis).
   * The loop accumulates results into the return variable.
   * </p>
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param result           The overall result variable for the control flow
   * @param returnDetails    Return variable details from returningParam processing
   * @param conditionChain   Loop condition with body (single element)
   * @param debugInfo        Debug information
   * @param scopeId          Scope ID from stack context
   * @return ControlFlowChainDetails configured for for-in expression
   */
  public static ControlFlowChainDetails createForInExpression(
      String result,
      ReturnVariableDetails returnDetails,
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "FOR_IN_EXPRESSION",
        GuardVariableDetails.none(), // Guards NOT supported in expression forms
        EvaluationVariableDetails.none(), // No evaluation variable for for-in
        returnDetails,
        conditionChain,
        DefaultCaseDetails.none(), // No default case for loops
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a while expression (result &lt;- while condition).
   * <p>
   * Expression forms return a value via the return variable declared in returningParam.
   * Guards are NOT supported in expression forms (enforced by semantic analysis).
   * </p>
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param result           The overall result variable for the control flow
   * @param returnDetails    Return variable details from returningParam processing
   * @param conditionChain   Loop condition with body (single element)
   * @param debugInfo        Debug information
   * @param scopeId          Scope ID from stack context
   * @return ControlFlowChainDetails configured for while expression
   */
  public static ControlFlowChainDetails createWhileExpression(
      String result,
      ReturnVariableDetails returnDetails,
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "WHILE_EXPRESSION",
        GuardVariableDetails.none(), // Guards NOT supported in expression forms
        EvaluationVariableDetails.none(), // No evaluation variable for while
        returnDetails,
        conditionChain,
        DefaultCaseDetails.none(), // No default case for loops
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a do-while expression (result &lt;- do { body } while condition).
   * <p>
   * Expression forms return a value via the return variable declared in returningParam.
   * Guards are NOT supported in expression forms (enforced by semantic analysis).
   * Body executes at least once before condition is checked.
   * </p>
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param result           The overall result variable for the control flow
   * @param returnDetails    Return variable details from returningParam processing
   * @param conditionChain   Loop body with condition (single element)
   * @param debugInfo        Debug information
   * @param scopeId          Scope ID from stack context
   * @return ControlFlowChainDetails configured for do-while expression
   */
  public static ControlFlowChainDetails createDoWhileExpression(
      String result,
      ReturnVariableDetails returnDetails,
      List<ConditionCaseDetails> conditionChain,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "DO_WHILE_EXPRESSION",
        GuardVariableDetails.none(), // Guards NOT supported in expression forms
        EvaluationVariableDetails.none(), // No evaluation variable for do-while
        returnDetails,
        conditionChain,
        DefaultCaseDetails.none(), // No default case for loops
        null, // No try block
        List.of(), // No finally block
        debugInfo,
        scopeId
    );
  }

  /**
   * Create details for a try-catch expression (result &lt;- try { expr } catch { handler }).
   * <p>
   * Expression forms return a value via the return variable declared in returningParam.
   * Guards are NOT supported in expression forms (enforced by semantic analysis).
   * </p>
   * STACK-BASED: scopeId parameter extracted from stack context in generator.
   *
   * @param result                 The overall result variable for the control flow
   * @param returnDetails          Return variable details from returningParam processing
   * @param tryBlockDetails        Try block scope and body evaluation
   * @param catchHandlers          List of catch handlers as condition cases
   * @param finallyBlockEvaluation Finally block instructions (empty if no finally)
   * @param debugInfo              Debug information
   * @param scopeId                Scope ID from stack context
   * @return ControlFlowChainDetails configured for try-catch expression
   */
  public static ControlFlowChainDetails createTryCatchExpression(
      String result,
      ReturnVariableDetails returnDetails,
      TryBlockDetails tryBlockDetails,
      List<ConditionCaseDetails> catchHandlers,
      List<IRInstr> finallyBlockEvaluation,
      DebugInfo debugInfo,
      String scopeId) {

    return new ControlFlowChainDetails(
        result,
        "TRY_CATCH_EXPRESSION",
        GuardVariableDetails.none(), // Guards NOT supported in expression forms
        EvaluationVariableDetails.none(), // No evaluation variable for try-catch
        returnDetails,
        catchHandlers,
        DefaultCaseDetails.none(), // No default case (finally is not a default)
        tryBlockDetails,
        finallyBlockEvaluation,
        debugInfo,
        scopeId
    );
  }

  // ================================================================================
  // End of Expression Form Factory Methods
  // ================================================================================

  /**
   * Get the scope ID extracted from stack context at Details creation time.
   * STACK-BASED: scopeId comes from IRGenerationContext.currentScopeId() where this object is created.
   */
  public String getScopeId() {
    return scopeId;
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
   * Check if this construct has a guard entry check (for WHILE/DO-WHILE loops).
   */
  public boolean hasGuardEntryCheck() {
    return guardDetails != null && guardDetails.hasGuardEntryCheck();
  }

  /**
   * Get guard entry check instructions (for WHILE/DO-WHILE loops).
   */
  public List<IRInstr> guardEntryCheck() {
    return guardDetails != null ? guardDetails.guardEntryCheck() : List.of();
  }

  /**
   * Get guard entry check primitive variable name (for WHILE/DO-WHILE loops).
   */
  public String guardEntryCheckPrimitive() {
    return guardDetails != null ? guardDetails.guardEntryCheckPrimitive() : null;
  }

  /**
   * Check if this construct has a default case.
   */
  public boolean hasDefaultCase() {
    return defaultDetails != null && defaultDetails.hasDefaultCase();
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
}