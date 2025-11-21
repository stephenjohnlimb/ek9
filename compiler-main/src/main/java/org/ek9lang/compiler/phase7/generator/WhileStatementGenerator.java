package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
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

  public WhileStatementGenerator(final IRGenerationContext stackContext,
                                 final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.guardedConditionEvaluator = new GuardedConditionEvaluator(stackContext, generators);
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
   * Follows the same two-scope pattern as if/else for architectural consistency.
   * Supports guards: while value <- getValue() then condition
   */
  private List<IRInstr> generateSimpleWhileLoop(
      final EK9Parser.WhileStatementExpressionContext ctx) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // SCOPE 1: Enter loop outer scope (for guards)
    // This matches if/else pattern exactly - outer wrapper for guards
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

    // Check for guard (preFlowStatement) and evaluate if present
    // Guard evaluates ONCE before loop starts (not each iteration)
    // Guard variable is scoped to the loop and available in condition and body
    if (ctx.preFlowStatement() != null) {
      evaluateGuardVariable(ctx.preFlowStatement(), instructions);
    }

    // SCOPE 2: Enter whole loop scope (loop control structure)
    final var wholeLoopScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(wholeLoopScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(wholeLoopScopeId, debugInfo));

    // SCOPE 3: Enter condition iteration scope (for tight temp management)
    // This scope enters/exits each iteration to release condition temps
    final var conditionIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process condition expression with scope management
    final var conditionResult = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<IRInstr>();

    // Enter condition iteration scope
    conditionEvaluation.add(ScopeInstr.enter(conditionIterationScopeId, debugInfo));

    // Evaluate guarded condition (handles all 4 cases: guard+condition, guard-only, condition-only, etc.)
    final var evaluation = guardedConditionEvaluator.evaluate(
        ctx.control,                  // Condition expression (may be null for guard-only)
        ctx.preFlowStatement(),       // Guard variable (may be null for condition-only)
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

    // Create CONTROL_FLOW_CHAIN with whole loop scope ID
    // Backend will interpret WHILE_LOOP to generate loop-back logic
    final var whileDetails = ControlFlowChainDetails.createWhileLoop(
        List.of(conditionCase),
        debugInfo,
        wholeLoopScopeId    // Whole loop scope ID
    );

    instructions.addAll(generators.controlFlowChainGenerator.apply(whileDetails));

    // Exit whole loop scope
    instructions.add(ScopeInstr.exit(wholeLoopScopeId, debugInfo));
    stackContext.exitScope();

    // Exit outer scope
    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate IR for do-while loop: do { body } while condition
   * Key difference: Body executes FIRST (at least once), then condition is evaluated.
   * Supports guards: do value <- getValue() { body } while condition
   */
  private List<IRInstr> generateDoWhileLoop(
      final EK9Parser.WhileStatementExpressionContext ctx) {

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // SCOPE 1: Enter loop outer scope (for guards)
    final var outerScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(outerScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(outerScopeId, debugInfo));

    // Check for guard (preFlowStatement) and evaluate if present
    // Guard evaluates ONCE before loop starts (not each iteration)
    // Guard variable is scoped to the loop and available in condition and body
    if (ctx.preFlowStatement() != null) {
      evaluateGuardVariable(ctx.preFlowStatement(), instructions);
    }

    // SCOPE 2: Enter whole loop scope (loop control structure)
    final var wholeLoopScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(wholeLoopScopeId, debugInfo, IRFrameType.BLOCK);
    instructions.add(ScopeInstr.enter(wholeLoopScopeId, debugInfo));

    // SCOPE 3: Body iteration scope (executes FIRST in do-while)
    final var bodyIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(bodyIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process body FIRST (key do-while characteristic)
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(bodyIterationScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.instructionBlock(), generators.blockStmtGenerator));
    bodyEvaluation.add(ScopeInstr.exit(bodyIterationScopeId, debugInfo));

    // Exit body scope from context
    stackContext.exitScope();

    // SCOPE 4: Condition evaluation scope (evaluated AFTER body)
    final var conditionIterationScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionIterationScopeId, debugInfo, IRFrameType.BLOCK);

    // Process condition expression (evaluated AFTER body)
    final var conditionResult = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<IRInstr>();

    // Enter condition iteration scope
    conditionEvaluation.add(ScopeInstr.enter(conditionIterationScopeId, debugInfo));

    // Evaluate guarded condition (same 4-case pattern as WHILE)
    final var evaluation = guardedConditionEvaluator.evaluate(
        ctx.control,                  // Condition expression (may be null for guard-only)
        ctx.preFlowStatement(),       // Guard variable (may be null for condition-only)
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

    // Create CONTROL_FLOW_CHAIN with DO_WHILE_LOOP type
    // Backend will interpret DO_WHILE_LOOP to generate correct loop structure
    final var doWhileDetails = ControlFlowChainDetails.createDoWhileLoop(
        List.of(conditionCase),
        debugInfo,
        wholeLoopScopeId
    );

    instructions.addAll(generators.controlFlowChainGenerator.apply(doWhileDetails));

    // Exit whole loop scope
    instructions.add(ScopeInstr.exit(wholeLoopScopeId, debugInfo));
    stackContext.exitScope();

    // Exit outer scope
    instructions.add(ScopeInstr.exit(outerScopeId, debugInfo));
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Evaluate guard variable for while/do-while loop.
   * Pattern: while value <- getValue() then condition
   * <p>
   * Guard is evaluated ONCE before the loop starts (not on each iteration).
   * Guard variable is scoped to the entire loop and available in both condition and body.
   * Uses existing variableDeclGenerator to handle REFERENCE, STORE, RETAIN, SCOPE_REGISTER.
   * </p>
   * <p>
   * NOTE: This implementation evaluates the guard but does NOT add isSet check wrapper.
   * Future enhancement: Wrap entire loop in guard isSet check (like IF guards do).
   * </p>
   *
   * @param preFlowStmt  preFlowStatement context with guard
   * @param instructions List to append guard evaluation instructions
   */
  private void evaluateGuardVariable(
      final EK9Parser.PreFlowStatementContext preFlowStmt,
      final List<IRInstr> instructions) {

    // Use existing variable declaration generator (handles REFERENCE, STORE, RETAIN, SCOPE_REGISTER)
    // Guard variable will be registered to the current scope (outer scope)
    if (preFlowStmt.variableDeclaration() != null) {
      instructions.addAll(generators.variableDeclGenerator.apply(preFlowStmt.variableDeclaration()));
      return;
    }

    // For now, only support variable declaration form (value <- expr)
    // Future: Support assignment (:=) and guarded assignment (?=) forms
    throw new org.ek9lang.core.CompilerException(
        "While/Do-while guards currently only support variable declaration form (value <- expr)");
  }

}
