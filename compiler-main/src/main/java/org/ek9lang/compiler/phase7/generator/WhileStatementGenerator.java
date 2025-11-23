package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.core.AssertValue;

/**
 * Generates IR for while and do-while loops using CONTROL_FLOW_CHAIN.
 * Currently handles simple while and do-while loops (no guards, statement form only).
 * <p>
 * Scope structure:
 * </p>
 * <pre>
 *   Outer Scope (_scope_1): Loop wrapper for future guards
 *   Whole Loop Scope (_scope_2): Loop control structure
 *   Condition Iteration Scope (_scope_3): Condition temps, enters/exits each iteration
 *   Body Iteration Scope (_scope_4): Body execution, enters/exits each iteration
 * </pre>
 * <p>
 * The outer scope pattern enables future guard and expression form support.
 * Both condition and body use iteration scopes for tight memory management.
 * </p>
 */
public final class WhileStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.WhileStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final GuardedConditionEvaluator guardedConditionEvaluator;
  private final LoopGuardHelper loopGuardHelper;

  public WhileStatementGenerator(final IRGenerationContext stackContext,
                                 final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.guardedConditionEvaluator = new GuardedConditionEvaluator(stackContext, generators);
    this.loopGuardHelper = new LoopGuardHelper(stackContext, generators);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.WhileStatementExpressionContext ctx) {
    AssertValue.checkNotNull("WhileStatementExpressionContext cannot be null", ctx);

    // Detect which form: while ... or do ... while
    if (ctx.DO() != null) {
      return generateDoWhileLoop(ctx);
    }

    // Check for expression form (returningParam)
    validateStatementFormOnly(ctx.returningParam(), "While loop");

    // Simple while loop (statement form)
    return generateSimpleWhileLoop(ctx);
  }

  /**
   * Generate IR for simple while loop: while condition { body }
   * Follows the same single-scope pattern as if/else for architectural consistency.
   * Supports guards: while value <- getValue() then condition
   * <p>
   * For guards with entry checks (?= and <- operators), the guard is checked ONCE at loop entry.
   * If the check fails, the entire loop is skipped. This is implemented by wrapping the loop
   * in an IF_ELSE_IF that checks the guard entry condition.
   * </p>
   * <p>
   * NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER/EXIT instructions
   * for the chain scope, so we don't add them here to avoid duplicates.
   * </p>
   */
  private List<IRInstr> generateSimpleWhileLoop(
      final EK9Parser.WhileStatementExpressionContext ctx) {

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter scope for entire while loop (contains guard variables and loop control)
    // NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER instruction
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    // Process guard if present (using shared helper)
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(ctx.preFlowStatement(), chainScopeId);

    // Generate the core while loop
    final var whileLoopInstructions = generateWhileLoopCore(ctx, guardDetails, chainScopeId, debugInfo);

    // If guard has entry check, wrap loop in IF that checks entry condition (using shared helper)
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      instructions = loopGuardHelper.wrapChainWithGuardEntryCheck(
          guardDetails, whileLoopInstructions, chainScopeId, debugInfo);
    } else {
      instructions = new ArrayList<>(generators.controlFlowChainGenerator.apply(whileLoopInstructions));
    }

    // Exit chain scope from stack context (IR instructions already added by ControlFlowChainGenerator)
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate the core while loop without entry check wrapping.
   */
  private ControlFlowChainDetails generateWhileLoopCore(
      final EK9Parser.WhileStatementExpressionContext ctx,
      final GuardVariableDetails guardDetails,
      final String chainScopeId,
      final org.ek9lang.compiler.ir.support.DebugInfo debugInfo) {

    // SCOPE 3 (or SCOPE 2): Enter condition iteration scope (for tight temp management)
    // This scope enters/exits each iteration to release condition temps
    final var conditionIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process condition expression with scope management
    final var conditionResult = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<IRInstr>();

    // Enter condition iteration scope
    conditionEvaluation.add(ScopeInstr.enter(conditionIterationScopeId, debugInfo));

    // Evaluate condition ONLY (guard check happens once at entry, not each iteration)
    // Pass null for preFlowStatement to get condition-only evaluation
    final var evaluation = guardedConditionEvaluator.evaluate(
        ctx.control,                  // Condition expression (REQUIRED for while loops)
        null,                         // NO guard check in condition - guard checked once at entry
        conditionResult,              // Result variable
        conditionIterationScopeId,    // Scope ID for condition evaluation
        debugInfo);                   // Debug information

    // Add the generated condition evaluation instructions
    conditionEvaluation.addAll(evaluation.instructions());
    final var primitiveCondition = evaluation.primitiveCondition();

    // Exit condition iteration scope
    conditionEvaluation.add(ScopeInstr.exit(conditionIterationScopeId, debugInfo));

    // Exit condition iteration scope from context
    stackContext.exitScope();

    // SCOPE 4: Enter body iteration scope
    // This scope enters/exits each iteration for body execution
    final var bodyIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(bodyIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process body with scope management
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(bodyIterationScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.instructionBlock(), generators.blockStmtGenerator));
    bodyEvaluation.add(ScopeInstr.exit(bodyIterationScopeId, debugInfo));

    // Exit body iteration scope from context
    stackContext.exitScope();

    // Create ConditionCaseDetails (single case for while loop)
    final var conditionCase = ConditionCaseDetails.createExpression(
        conditionIterationScopeId,            // case_scope_id (condition iteration scope)
        conditionEvaluation,                  // condition with SCOPE_ENTER/EXIT
        conditionResult.resultVariable(),     // EK9 Boolean result
        primitiveCondition,                   // primitive boolean for branching
        bodyEvaluation,                       // body with SCOPE_ENTER/EXIT
        null                                  // no result (statement form)
    );

    // Create CONTROL_FLOW_CHAIN with chain scope ID
    // Uses createWhileLoopWithGuards to produce WHILE_LOOP_WITH_GUARDS when guards present
    // Backend will interpret WHILE_LOOP or WHILE_LOOP_WITH_GUARDS to generate loop-back logic
    return ControlFlowChainDetails.createWhileLoopWithGuards(
        guardDetails,
        List.of(conditionCase),
        debugInfo,
        chainScopeId    // Chain scope ID (same pattern as IF)
    );
  }

  /**
   * Generate IR for do-while loop: do { body } while condition
   * Key difference: Body executes FIRST (at least once), then condition is evaluated.
   * Follows the same single-scope pattern as if/else for architectural consistency.
   * Supports guards: do value <- getValue() { body } while condition
   * <p>
   * For guards with entry checks (?= and <- operators), the guard is checked ONCE at loop entry.
   * If the check fails, the entire loop is skipped. This is implemented by wrapping the loop
   * in an IF_ELSE_IF that checks the guard entry condition.
   * </p>
   * <p>
   * NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER/EXIT instructions
   * for the chain scope, so we don't add them here to avoid duplicates.
   * </p>
   */
  private List<IRInstr> generateDoWhileLoop(
      final EK9Parser.WhileStatementExpressionContext ctx) {

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter scope for entire do-while loop (contains guard variables and loop control)
    // NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER instruction
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    // Process guard if present (using shared helper)
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(ctx.preFlowStatement(), chainScopeId);

    // Generate the core do-while loop
    final var doWhileLoopInstructions = generateDoWhileLoopCore(ctx, guardDetails, chainScopeId, debugInfo);

    // If guard has entry check, wrap loop in IF that checks entry condition (using shared helper)
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      instructions = loopGuardHelper.wrapChainWithGuardEntryCheck(
          guardDetails, doWhileLoopInstructions, chainScopeId, debugInfo);
    } else {
      instructions = new ArrayList<>(generators.controlFlowChainGenerator.apply(doWhileLoopInstructions));
    }

    // Exit chain scope from stack context (IR instructions already added by ControlFlowChainGenerator)
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate the core do-while loop without entry check wrapping.
   */
  private ControlFlowChainDetails generateDoWhileLoopCore(
      final EK9Parser.WhileStatementExpressionContext ctx,
      final GuardVariableDetails guardDetails,
      final String chainScopeId,
      final org.ek9lang.compiler.ir.support.DebugInfo debugInfo) {

    // Body iteration scope (executes FIRST in do-while)
    final var bodyIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(bodyIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process body FIRST (key do-while characteristic)
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(bodyIterationScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.instructionBlock(), generators.blockStmtGenerator));
    bodyEvaluation.add(ScopeInstr.exit(bodyIterationScopeId, debugInfo));

    // Exit body scope from context
    stackContext.exitScope();

    // Condition evaluation scope (evaluated AFTER body)
    final var conditionIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process condition expression (evaluated AFTER body)
    final var conditionResult = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<IRInstr>();

    // Enter condition iteration scope
    conditionEvaluation.add(ScopeInstr.enter(conditionIterationScopeId, debugInfo));

    // Evaluate condition ONLY (guard check happens once at entry, not each iteration)
    // Pass null for preFlowStatement to get condition-only evaluation
    final var evaluation = guardedConditionEvaluator.evaluate(
        ctx.control,                  // Condition expression (REQUIRED for do-while loops)
        null,                         // NO guard check in condition - guard checked once at entry
        conditionResult,              // Result variable
        conditionIterationScopeId,    // Scope ID for condition evaluation
        debugInfo);                   // Debug information

    // Add the generated condition evaluation instructions
    conditionEvaluation.addAll(evaluation.instructions());
    final var primitiveCondition = evaluation.primitiveCondition();

    // Exit condition iteration scope
    conditionEvaluation.add(ScopeInstr.exit(conditionIterationScopeId, debugInfo));

    // Exit condition scope from context
    stackContext.exitScope();

    // Create ConditionCaseDetails for do-while
    // Body comes first, then condition
    final var conditionCase = ConditionCaseDetails.createExpression(
        bodyIterationScopeId,                 // case_scope_id (body scope for do-while)
        conditionEvaluation,                  // condition evaluated AFTER body
        conditionResult.resultVariable(),     // EK9 Boolean result
        primitiveCondition,                   // primitive boolean for branching
        bodyEvaluation,                       // body executes FIRST
        null                                  // no result (statement form)
    );

    // Create CONTROL_FLOW_CHAIN with chain scope ID
    // Uses createDoWhileLoopWithGuards to produce DO_WHILE_LOOP_WITH_GUARDS when guards present
    return ControlFlowChainDetails.createDoWhileLoopWithGuards(
        guardDetails,
        List.of(conditionCase),
        debugInfo,
        chainScopeId    // Chain scope ID (same pattern as IF)
    );
  }

}
