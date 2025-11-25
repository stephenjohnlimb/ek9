package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableNameForIR;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Shared helper for guard variable evaluation in control flow constructs.
 * <p>
 * Provides reusable guard handling for:
 * </p>
 * <ul>
 *   <li>WHILE and DO-WHILE loops</li>
 *   <li>FOR-RANGE loops</li>
 *   <li>FOR-IN loops</li>
 *   <li>TRY statements</li>
 * </ul>
 * <p>
 * All constructs use the same guard pattern:
 * </p>
 * <ol>
 *   <li>Enter guard scope</li>
 *   <li>Guard setup (evaluates expression, assigns to variable)</li>
 *   <li>Guard entry check (IS_NULL + _isSet produces Boolean)</li>
 *   <li>IF (entry check passes) { inner_body }</li>
 *   <li>Exit guard scope</li>
 * </ol>
 * <p>
 * Supports all 4 guard operators:
 * </p>
 * <ul>
 *   <li>{@code <-} (Declaration): Creates new variable, WITH guard check</li>
 *   <li>{@code :=} (Assignment): Assigns to existing variable, NO guard check</li>
 *   <li>{@code :=?} (Assignment If Unset): Assigns if unset, WITH guard check</li>
 *   <li>{@code ?=} (Guarded Assignment): Assigns and checks result, WITH guard check</li>
 * </ul>
 */
public final class LoopGuardHelper extends AbstractGenerator {

  private final GeneratorSet generators;
  private final GuardedConditionEvaluator guardedConditionEvaluator;

  public LoopGuardHelper(final IRGenerationContext stackContext,
                         final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.guardedConditionEvaluator = new GuardedConditionEvaluator(stackContext, generators);
  }

  /**
   * Evaluate guard variable for control flow constructs.
   * Supports all guard forms (same as IF statements):
   * <ul>
   *   <li>{@code <-} (Declaration): {@code while value <- getValue() then condition}</li>
   *   <li>{@code :=} (Assignment): {@code while value := getValue() then condition} - NO guard check</li>
   *   <li>{@code :=?} (Assignment If Unset): {@code while value :=? getValue() then condition}</li>
   *   <li>{@code ?=} (Guarded Assignment): {@code while value ?= getValue() then condition}</li>
   * </ul>
   * <p>
   * Guard is evaluated ONCE before the construct body executes (not on each iteration for loops).
   * Guard variable is scoped to the entire construct and available in body.
   * </p>
   * <p>
   * For loops and TRY, this also generates the guard ENTRY CHECK - the IS_NULL + _isSet()
   * evaluation that happens ONCE at entry. If the check fails, the entire body is skipped.
   * </p>
   * <p>
   * NOTE: For {@code :=} (blind assignment), NO entry check is generated since the operator
   * doesn't require guard validation - it's just an assignment before the body.
   * </p>
   *
   * @param preFlowStmt  preFlowStatement context with guard (may be null)
   * @param guardScopeId Scope ID where guard variable lives
   * @return GuardVariableDetails with guard info including entry check, or GuardVariableDetails.none() if no guard
   */
  public GuardVariableDetails evaluateGuardVariable(
      final EK9Parser.PreFlowStatementContext preFlowStmt,
      final String guardScopeId) {

    // No guard present - return empty guard details
    if (preFlowStmt == null) {
      return GuardVariableDetails.none();
    }

    final var guardSetup = new ArrayList<IRInstr>();
    final var guardVariables = new ArrayList<String>();
    final var debugInfo = stackContext.createDebugInfo(preFlowStmt);

    // Determine operator type and whether guard check is needed
    final var operatorType = guardedConditionEvaluator.getGuardOperatorType(preFlowStmt);
    final var needsGuardCheck = guardedConditionEvaluator.requiresGuardCheck(operatorType);

    // Handle variable declaration (<-): if value <- getValue()
    if (preFlowStmt.variableDeclaration() != null) {
      guardSetup.addAll(generators.variableDeclGenerator.apply(preFlowStmt.variableDeclaration()));

      final var guardSymbol = getRecordedSymbolOrException(preFlowStmt.variableDeclaration());
      final var variableName = new VariableNameForIR().apply(guardSymbol);

      if (needsGuardCheck) {
        guardVariables.add(variableName);
        final var entryCheckResult = generateGuardEntryCheck(guardSymbol, variableName, guardScopeId, debugInfo);
        return GuardVariableDetails.createWithEntryCheck(
            guardVariables, guardSetup, entryCheckResult.conditionEval().instructions(),
            entryCheckResult.conditionEval().primitiveCondition(),
            entryCheckResult.resultVariable(), guardScopeId);
      } else {
        // No guard check needed (e.g., := or = operator in variable declaration)
        return GuardVariableDetails.create(guardVariables, guardSetup, guardScopeId, null);
      }
    }

    // Handle assignment statement (:= or :=?): while value := getValue() or while value :=? getValue()
    if (preFlowStmt.assignmentStatement() != null) {
      guardSetup.addAll(generators.assignmentStmtGenerator.apply(preFlowStmt.assignmentStatement()));

      final var assignSymbol = getRecordedSymbolOrException(preFlowStmt.assignmentStatement().identifier());
      final var variableName = new VariableNameForIR().apply(assignSymbol);

      if (needsGuardCheck) {
        guardVariables.add(variableName);
        final var entryCheckResult = generateGuardEntryCheck(assignSymbol, variableName, guardScopeId, debugInfo);
        return GuardVariableDetails.createWithEntryCheck(
            guardVariables, guardSetup, entryCheckResult.conditionEval().instructions(),
            entryCheckResult.conditionEval().primitiveCondition(),
            entryCheckResult.resultVariable(), guardScopeId);
      } else {
        // := (blind assignment) - no guard check, just setup
        return GuardVariableDetails.create(guardVariables, guardSetup, guardScopeId, null);
      }
    }

    // Handle guard expression (?=): while value ?= getValue()
    if (preFlowStmt.guardExpression() != null) {
      final var guardExpr = preFlowStmt.guardExpression();

      // Get target variable symbol
      final var targetSymbol = getRecordedSymbolOrException(guardExpr.identifier());
      final var targetName = new VariableNameForIR().apply(targetSymbol);

      // Create temp variable for expression result
      final var tempDetails = createTempVariable(debugInfo);

      // Evaluate the expression (RHS) into temp variable
      guardSetup.addAll(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new ExprProcessingDetails(guardExpr.expression(), tempDetails)
              ),
              tempDetails
          )
      );

      // Assign temp to target variable
      guardSetup.add(MemoryInstr.release(targetName, debugInfo));
      guardSetup.add(MemoryInstr.store(targetName, tempDetails.resultVariable(), debugInfo));
      guardSetup.add(MemoryInstr.retain(targetName, debugInfo));

      // ?= always requires guard check
      guardVariables.add(targetName);
      final var entryCheckResult = generateGuardEntryCheck(targetSymbol, targetName, guardScopeId, debugInfo);
      return GuardVariableDetails.createWithEntryCheck(
          guardVariables, guardSetup, entryCheckResult.conditionEval().instructions(),
          entryCheckResult.conditionEval().primitiveCondition(),
          entryCheckResult.resultVariable(), guardScopeId);
    }

    throw new CompilerException(
        "Invalid preFlowStatement - expected variableDeclaration, assignmentStatement, or guardExpression");
  }

  /**
   * Wrap body instructions in an IF_ELSE_IF that checks the guard entry condition.
   * <p>
   * Structure:
   * </p>
   * <ol>
   *   <li>SCOPE_ENTER (guard scope)</li>
   *   <li>Guard setup (assignment)</li>
   *   <li>Guard entry check (IS_NULL + _isSet)</li>
   *   <li>IF (entry check passes) { body }</li>
   *   <li>SCOPE_EXIT (guard scope)</li>
   * </ol>
   *
   * @param guardDetails     Guard variable details with entry check info
   * @param bodyInstructions The body instructions to wrap (loop or try block)
   * @param guardScopeId     The scope ID for the guard
   * @param debugInfo        Debug information
   * @return Instructions with guard wrapper
   */
  public List<IRInstr> wrapBodyWithGuardEntryCheck(
      final GuardVariableDetails guardDetails,
      final List<IRInstr> bodyInstructions,
      final String guardScopeId,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // 1. Enter guard scope
    instructions.add(ScopeInstr.enter(guardScopeId, debugInfo));

    // 2. Guard setup (evaluates expression and assigns to guard variable)
    instructions.addAll(guardDetails.guardScopeSetup());

    // 3. Guard entry check (IS_NULL + _isSet) - produces primitive boolean
    instructions.addAll(guardDetails.guardEntryCheck());

    // 4. Create IF wrapper: if (entryCheckPasses) { body }
    // The body goes in the IF body, no else needed (skip body if entry check fails)

    // Create IF condition case with entry check as condition
    // Include the EK9 Boolean result for condition_result to enable backend optimizations
    final var ifConditionCase = ConditionCaseDetails.createExpression(
        guardScopeId,                           // case_scope_id
        List.of(),                              // condition evaluation (already done above)
        guardDetails.guardEntryCheckResult(),   // EK9 Boolean result for condition_result
        guardDetails.guardEntryCheckPrimitive(), // primitive boolean from entry check
        bodyInstructions,                       // body wrapped in IF
        null                                    // no result (statement form)
    );

    // Create IF_ELSE_IF control flow chain (no default/else - just skip body if false)
    final var ifDetails = ControlFlowChainDetails.createIfElseWithGuards(
        null,                                   // no result
        GuardVariableDetails.none(),            // guards already handled above
        List.of(ifConditionCase),
        List.of(),                              // no default body
        null,                                   // no default result
        debugInfo,
        guardScopeId
    );

    // Add IF control flow chain instruction
    instructions.add(ControlFlowChainInstr.controlFlowChain(ifDetails));

    // 5. Exit guard scope
    instructions.add(ScopeInstr.exit(guardScopeId, debugInfo));

    return instructions;
  }

  /**
   * Wrap a ControlFlowChainDetails (loop or try) in an IF_ELSE_IF that checks the guard entry condition.
   * <p>
   * This is a convenience method for wrapping CONTROL_FLOW_CHAIN instructions.
   * </p>
   *
   * @param guardDetails Guard variable details with entry check info
   * @param chainDetails The control flow chain to wrap
   * @param guardScopeId The scope ID for the guard
   * @param debugInfo    Debug information
   * @return Instructions with guard wrapper
   */
  public List<IRInstr> wrapChainWithGuardEntryCheck(
      final GuardVariableDetails guardDetails,
      final ControlFlowChainDetails chainDetails,
      final String guardScopeId,
      final DebugInfo debugInfo) {

    // Wrap the chain instruction in a list for the body
    final var bodyInstructions = new ArrayList<IRInstr>();
    bodyInstructions.add(ControlFlowChainInstr.controlFlowChain(chainDetails));

    return wrapBodyWithGuardEntryCheck(guardDetails, bodyInstructions, guardScopeId, debugInfo);
  }

  /**
   * Wrap body instructions in an IF_ELSE_IF for EXPRESSION FORM with return variable.
   * <p>
   * CRITICAL: For expression forms, the return variable must be initialized OUTSIDE the IF wrapper
   * so the variable exists on all code paths (including when guard fails). This prevents JVM
   * VerifyError "Bad local variable type" when accessing the return variable after the IF.
   * </p>
   * <p>
   * Structure:
   * </p>
   * <ol>
   *   <li>SCOPE_ENTER (guard scope)</li>
   *   <li>Guard setup (assignment)</li>
   *   <li>Guard entry check (IS_NULL + _isSet)</li>
   *   <li>Return variable setup (OUTSIDE IF - always executed)</li>
   *   <li>IF (entry check passes) { body only }</li>
   *   <li>SCOPE_EXIT (guard scope)</li>
   * </ol>
   *
   * @param guardDetails             Guard variable details with entry check info
   * @param returnVariableSetup      Return variable initialization instructions (expression form)
   * @param bodyInstructionsWithoutRtn Body instructions WITHOUT return variable setup
   * @param guardScopeId             The scope ID for the guard
   * @param debugInfo                Debug information
   * @return Instructions with guard wrapper and properly placed return variable
   */
  public List<IRInstr> wrapExpressionFormWithGuardEntryCheck(
      final GuardVariableDetails guardDetails,
      final List<IRInstr> returnVariableSetup,
      final List<IRInstr> bodyInstructionsWithoutRtn,
      final String guardScopeId,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // 1. Enter guard scope
    instructions.add(ScopeInstr.enter(guardScopeId, debugInfo));

    // 2. Guard setup (evaluates expression and assigns to guard variable)
    instructions.addAll(guardDetails.guardScopeSetup());

    // 3. Guard entry check (IS_NULL + _isSet) - produces primitive boolean
    instructions.addAll(guardDetails.guardEntryCheck());

    // 4. Return variable setup - OUTSIDE IF, always executed so variable exists on all paths
    instructions.addAll(returnVariableSetup);

    // 5. Create IF wrapper: if (entryCheckPasses) { body only }
    // Body does NOT include return variable setup - it's already done above
    final var ifConditionCase = ConditionCaseDetails.createExpression(
        guardScopeId,                           // case_scope_id
        List.of(),                              // condition evaluation (already done above)
        guardDetails.guardEntryCheckResult(),   // EK9 Boolean result for condition_result
        guardDetails.guardEntryCheckPrimitive(), // primitive boolean from entry check
        bodyInstructionsWithoutRtn,             // body WITHOUT return variable setup
        null                                    // no result (statement form)
    );

    // Create IF_ELSE_IF control flow chain (no default/else - just skip body if false)
    final var ifDetails = ControlFlowChainDetails.createIfElseWithGuards(
        null,                                   // no result
        GuardVariableDetails.none(),            // guards already handled above
        List.of(ifConditionCase),
        List.of(),                              // no default body
        null,                                   // no default result
        debugInfo,
        guardScopeId
    );

    // Add IF control flow chain instruction
    instructions.add(ControlFlowChainInstr.controlFlowChain(ifDetails));

    // 6. Exit guard scope
    instructions.add(ScopeInstr.exit(guardScopeId, debugInfo));

    return instructions;
  }

  /**
   * Generate guard entry check instructions for a guard variable.
   * This evaluates IS_NULL + _isSet() ONCE at entry.
   * If the check fails (null or not set), the body should be skipped.
   *
   * @param guardSymbol  The guard variable symbol
   * @param variableName The guard variable name in IR
   * @param scopeId      The scope ID for the check
   * @param debugInfo    Debug information
   * @return GuardEntryCheckResult containing instructions, primitive condition, and Boolean result variable
   */
  private GuardEntryCheckResult generateGuardEntryCheck(
      final ISymbol guardSymbol,
      final String variableName,
      final String scopeId,
      final DebugInfo debugInfo) {

    // Use the GuardedConditionEvaluator to generate the guard check
    // Pass null for condition expression since we only want the guard check
    // The guard-only case (case 4) generates QUESTION_OPERATOR (IS_NULL + _isSet)
    final var tempResult = createTempVariable(debugInfo);

    final var conditionEval = guardedConditionEvaluator.evaluateGuardOnlyForVariable(
        guardSymbol,
        variableName,
        tempResult,
        scopeId,
        debugInfo);

    return new GuardEntryCheckResult(conditionEval, tempResult.resultVariable());
  }

  /**
   * Result of guard entry check generation, containing both the condition evaluation
   * and the EK9 Boolean result variable name for use in condition_result.
   *
   * @param conditionEval  The condition evaluation with instructions and primitive condition
   * @param resultVariable The EK9 Boolean result variable name for condition_result in IF wrapper
   */
  private record GuardEntryCheckResult(
      GuardedConditionEvaluator.ConditionEvaluation conditionEval,
      String resultVariable) {
  }
}
