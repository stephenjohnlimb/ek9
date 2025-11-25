package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.data.GuardVariableDetails;
import org.ek9lang.compiler.ir.data.ReturnVariableDetails;
import org.ek9lang.compiler.ir.instructions.ControlFlowChainInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.ScopeInstr;
import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.generation.IRFrameType;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.core.AssertValue;

/**
 * Generates IR for while and do-while loops and expressions using CONTROL_FLOW_CHAIN.
 * <p>
 * Supports both statement and expression forms:
 * </p>
 * <ul>
 *   <li>Statement form: {@code while condition { body }} - no return value</li>
 *   <li>Expression form: {@code result <- while condition <- rtn <- 0 { body }} - accumulator pattern</li>
 * </ul>
 * <p>
 * Scope structure:
 * </p>
 * <pre>
 *   Outer Scope (_scope_1): Loop wrapper for guards + return variable (expression form)
 *   Whole Loop Scope (_scope_2): Loop control structure
 *   Condition Iteration Scope (_scope_3): Condition temps, enters/exits each iteration
 *   Body Iteration Scope (_scope_4): Body execution, enters/exits each iteration
 * </pre>
 * <p>
 * The outer scope pattern enables guard and expression form support.
 * Both condition and body use iteration scopes for tight memory management.
 * </p>
 */
public final class WhileStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.WhileStatementExpressionContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final GuardedConditionEvaluator guardedConditionEvaluator;
  private final LoopGuardHelper loopGuardHelper;
  private final ReturningParamProcessor returningParamProcessor;

  public WhileStatementGenerator(final IRGenerationContext stackContext,
                                 final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.guardedConditionEvaluator = new GuardedConditionEvaluator(stackContext, generators);
    this.loopGuardHelper = new LoopGuardHelper(stackContext, generators);
    this.returningParamProcessor = new ReturningParamProcessor(stackContext, generators);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.WhileStatementExpressionContext ctx) {
    AssertValue.checkNotNull("WhileStatementExpressionContext cannot be null", ctx);

    // Detect expression form via returningParam
    final var isExpressionForm = returningParamProcessor.isExpressionForm(ctx.returningParam());

    // Detect which form: while ... or do ... while
    if (ctx.DO() != null) {
      return generateDoWhileLoop(ctx, isExpressionForm);
    }

    // while loop (statement or expression form)
    return generateSimpleWhileLoop(ctx, isExpressionForm);
  }

  /**
   * Generate IR for simple while loop: while condition { body }
   * Follows the same single-scope pattern as if/else for architectural consistency.
   * Supports guards: while value <- getValue() then condition
   * <p>
   * Supports both statement and expression forms. For expression form, the return
   * variable is declared in the outer scope and accumulates values during the loop.
   * </p>
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
      final EK9Parser.WhileStatementExpressionContext ctx,
      final boolean isExpressionForm) {

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter scope for entire while loop (contains guard variables, return variable, and loop control)
    // NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER instruction
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    // Process guard if present (using shared helper)
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(ctx.preFlowStatement(), chainScopeId);

    // Process return variable for expression form (declared in outer scope)
    final var returnDetails = returningParamProcessor.process(ctx.returningParam(), chainScopeId);

    // Generate the core while loop (return variable setup handled by returnDetails)
    final var whileLoopInstructions = generateWhileLoopCore(ctx, guardDetails, returnDetails, chainScopeId, debugInfo);

    // If guard has entry check, wrap loop in IF that checks entry condition (using shared helper)
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      if (returnDetails.hasReturnVariable()) {
        // Expression form with guards: return variable setup AFTER guard check but BEFORE IF
        // This ensures rtn is always initialized regardless of whether the IF executes
        // Cannot use wrapBodyWithGuardEntryCheck because it puts everything inside IF body
        instructions = buildExpressionFormWithGuard(guardDetails, returnDetails,
            whileLoopInstructions, chainScopeId, debugInfo);
      } else {
        // Statement form with guards: no return variable setup needed
        instructions = loopGuardHelper.wrapChainWithGuardEntryCheck(
            guardDetails, whileLoopInstructions, chainScopeId, debugInfo);
      }
    } else {
      final var generatedInstructions = new ArrayList<IRInstr>();
      // For expression form, emit return variable setup instructions first
      generatedInstructions.addAll(returnDetails.returnVariableSetup());
      generatedInstructions.addAll(generators.controlFlowChainGenerator.apply(whileLoopInstructions));
      instructions = generatedInstructions;
    }

    // Exit chain scope from stack context (IR instructions already added by ControlFlowChainGenerator)
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate the core while loop without entry check wrapping.
   * <p>
   * For expression form, the return variable is already declared in outer scope.
   * The body statements include assignments to the return variable (accumulator pattern).
   * </p>
   */
  private ControlFlowChainDetails generateWhileLoopCore(
      final EK9Parser.WhileStatementExpressionContext ctx,
      final GuardVariableDetails guardDetails,
      final ReturnVariableDetails returnDetails,
      final String chainScopeId,
      final DebugInfo debugInfo) {

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

    // Create CONTROL_FLOW_CHAIN with chain scope ID using unified factory
    // Automatically selects expression vs statement form based on returnDetails
    // Backend will interpret chain type to generate appropriate loop logic
    return ControlFlowChainDetails.createWhileLoopUnified(
        guardDetails,
        returnDetails,
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
      final EK9Parser.WhileStatementExpressionContext ctx,
      final boolean isExpressionForm) {

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter scope for entire do-while loop (contains guard variables, return variable, and loop control)
    // NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER instruction
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    // Process guard if present (using shared helper)
    final var guardDetails = loopGuardHelper.evaluateGuardVariable(ctx.preFlowStatement(), chainScopeId);

    // Process return variable for expression form (declared in outer scope)
    final var returnDetails = returningParamProcessor.process(ctx.returningParam(), chainScopeId);

    // Generate the core do-while loop
    final var doWhileLoopInstructions =
        generateDoWhileLoopCore(ctx, guardDetails, returnDetails, chainScopeId, debugInfo);

    // If guard has entry check, wrap loop in IF that checks entry condition (using shared helper)
    final List<IRInstr> instructions;
    if (guardDetails.hasGuardEntryCheck()) {
      if (returnDetails.hasReturnVariable()) {
        // Expression form with guards: return variable setup AFTER guard check but BEFORE IF
        // This ensures rtn is always initialized regardless of whether the IF executes
        // Cannot use wrapBodyWithGuardEntryCheck because it puts everything inside IF body
        instructions = buildExpressionFormWithGuard(guardDetails, returnDetails,
            doWhileLoopInstructions, chainScopeId, debugInfo);
      } else {
        // Statement form with guards: no return variable setup needed
        instructions = loopGuardHelper.wrapChainWithGuardEntryCheck(
            guardDetails, doWhileLoopInstructions, chainScopeId, debugInfo);
      }
    } else {
      final var generatedInstructions = new ArrayList<IRInstr>();
      // For expression form, emit return variable setup instructions first
      generatedInstructions.addAll(returnDetails.returnVariableSetup());
      generatedInstructions.addAll(generators.controlFlowChainGenerator.apply(doWhileLoopInstructions));
      instructions = generatedInstructions;
    }

    // Exit chain scope from stack context (IR instructions already added by ControlFlowChainGenerator)
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Generate the core do-while loop without entry check wrapping.
   * <p>
   * For expression form, the return variable is already declared in outer scope.
   * The body statements include assignments to the return variable (accumulator pattern).
   * </p>
   */
  private ControlFlowChainDetails generateDoWhileLoopCore(
      final EK9Parser.WhileStatementExpressionContext ctx,
      final GuardVariableDetails guardDetails,
      final ReturnVariableDetails returnDetails,
      final String chainScopeId,
      final DebugInfo debugInfo) {

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

    // Create CONTROL_FLOW_CHAIN with chain scope ID using unified factory
    // Automatically selects expression vs statement form based on returnDetails
    return ControlFlowChainDetails.createDoWhileLoopUnified(
        guardDetails,
        returnDetails,
        List.of(conditionCase),
        debugInfo,
        chainScopeId    // Chain scope ID (same pattern as IF)
    );
  }

  /**
   * Build expression form with guard entry check.
   * <p>
   * For expression forms with guards, we need a specific structure:
   * </p>
   * <pre>
   *   SCOPE_ENTER guardScopeId
   *   guardSetup                  // Evaluate guard variable
   *   guardEntryCheck             // Check if guard is valid
   *   returnVariableSetup         // Initialize rtn (OUTSIDE IF - always executed)
   *   IF_ELSE_IF (entryCheck) {
   *     body: WHILE/DO_WHILE chain only
   *   }
   *   SCOPE_EXIT guardScopeId
   * </pre>
   * <p>
   * This ensures the return variable is always initialized, even if the guard
   * check fails and the IF body doesn't execute. The JVM requires all local
   * variables to be initialized on all code paths.
   * </p>
   *
   * @param guardDetails  Guard variable details with entry check
   * @param returnDetails Return variable details with setup instructions
   * @param loopChain     The WHILE or DO_WHILE loop chain
   * @param guardScopeId  The scope ID for the guard
   * @param debugInfo     Debug information
   * @return Instructions with proper structure
   */
  private List<IRInstr> buildExpressionFormWithGuard(
      final GuardVariableDetails guardDetails,
      final ReturnVariableDetails returnDetails,
      final ControlFlowChainDetails loopChain,
      final String guardScopeId,
      final DebugInfo debugInfo) {

    final var instructions = new ArrayList<IRInstr>();

    // 1. Enter guard scope
    instructions.add(ScopeInstr.enter(guardScopeId, debugInfo));

    // 2. Guard setup (evaluates expression and assigns to guard variable)
    instructions.addAll(guardDetails.guardScopeSetup());

    // 3. Guard entry check (IS_NULL + _isSet) - produces primitive boolean
    instructions.addAll(guardDetails.guardEntryCheck());

    // 4. Return variable setup (INSIDE scope, but OUTSIDE IF - always executed)
    // This ensures rtn is initialized on all code paths
    instructions.addAll(returnDetails.returnVariableSetup());

    // 5. Create IF wrapper: if (entryCheckPasses) { loopChain }
    // Only the loop chain goes in the IF body - rtn is already initialized above
    final var ifConditionCase = ConditionCaseDetails.createExpression(
        guardScopeId,                            // case_scope_id
        List.of(),                               // condition evaluation (already done above)
        guardDetails.guardEntryCheckResult(),    // EK9 Boolean result for condition_result
        guardDetails.guardEntryCheckPrimitive(), // primitive boolean from entry check
        List.of(ControlFlowChainInstr.controlFlowChain(loopChain)),  // body: loop chain only
        null                                     // no result (statement form)
    );

    // Create IF_ELSE_IF control flow chain (no default/else - just skip body if false)
    final var ifDetails = ControlFlowChainDetails.createIfElseWithGuards(
        null,                                    // no result
        GuardVariableDetails.none(),             // guards already handled above
        List.of(ifConditionCase),
        List.of(),                               // no default body
        null,                                    // no default result
        debugInfo,
        guardScopeId
    );

    // Add IF control flow chain instruction
    instructions.add(ControlFlowChainInstr.controlFlowChain(ifDetails));

    // 6. Exit guard scope
    instructions.add(ScopeInstr.exit(guardScopeId, debugInfo));

    return instructions;
  }

}
