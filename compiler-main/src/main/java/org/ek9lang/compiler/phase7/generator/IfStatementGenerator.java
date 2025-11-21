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
 * Generates IR for if/else statements using CONTROL_FLOW_CHAIN.
 * Uses GeneratorSet pattern for dependency injection.
 * <p>
 * Transforms EK9 if statements into CONTROL_FLOW_CHAIN instructions that
 * work equally well for JVM (stack-based) and LLVM (SSA-based) backends.
 * </p>
 */
public final class IfStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.IfStatementContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final GuardedConditionEvaluator guardedConditionEvaluator;

  public IfStatementGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
    this.guardedConditionEvaluator = new GuardedConditionEvaluator(stackContext, generators);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.IfStatementContext ctx) {
    AssertValue.checkNotNull("IfStatementContext cannot be null", ctx);

    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter NEW scope for entire if/else chain
    // This scope will contain guard variables and condition temporaries
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    // NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER instruction
    // for guard scope, so we don't add it here to avoid duplicate

    // Accumulate guard variables from ALL if/else-if blocks
    // Each block can have its own guard that lives in the chain scope
    final var allGuardVariables = new ArrayList<String>();
    final var allGuardSetup = new ArrayList<IRInstr>();

    // Process all if/else if conditions with UNIQUE branch scopes
    final var conditionChain = new ArrayList<ConditionCaseDetails>();
    for (var ifControlBlock : ctx.ifControlBlock()) {
      // Process guard for THIS specific block (if present)
      final var blockGuard = processGuardVariableIfPresent(ifControlBlock, chainScopeId);

      // Accumulate guard variables and setup from this block
      // NOTE: Setup (guardScopeSetup) includes declarations AND assignments
      // Guard variables (guardVariables) only includes vars that need guard checks
      // For := (blind assignment), setup is non-empty but guardVariables is empty
      if (blockGuard.hasGuardVariables()) {
        allGuardVariables.addAll(blockGuard.guardVariables());
      }
      if (!blockGuard.guardScopeSetup().isEmpty()) {
        allGuardSetup.addAll(blockGuard.guardScopeSetup());
      }

      // Process this block's condition with its specific guard
      conditionChain.add(processIfControlBlockWithBranchScope(ifControlBlock, blockGuard));
    }

    // Create combined guard details from all blocks
    // NOTE: For := (blind assignment), allGuardVariables is empty but allGuardSetup has the assignment
    // So we need to check guardSetup, not guardVariables!
    final var guardDetails = allGuardSetup.isEmpty()
        ? GuardVariableDetails.none()
        : GuardVariableDetails.create(allGuardVariables, allGuardSetup, chainScopeId, null);

    // Process else block with its OWN scope
    List<IRInstr> defaultBodyEvaluation = List.of();
    if (ctx.elseOnlyBlock() != null) {
      defaultBodyEvaluation = processElseBlockWithBranchScope(ctx.elseOnlyBlock());
    }

    // Create CONTROL_FLOW_CHAIN details with guard support
    final var details = ControlFlowChainDetails.createIfElseWithGuards(
        null, // No result for statement form
        guardDetails,
        conditionChain,
        defaultBodyEvaluation,
        null, // No default result for statement form
        debugInfo,
        chainScopeId
    );

    // Use ControlFlowChainGenerator to generate IR
    // NOTE: ControlFlowChainGenerator.apply() will add SCOPE_ENTER and SCOPE_EXIT
    // instructions for guard scope, so we don't add them here
    final var instructions = new ArrayList<>(generators.controlFlowChainGenerator.apply(details));

    // Exit chain scope from stack context (IR instructions already added by ControlFlowChainGenerator)
    stackContext.exitScope();

    return instructions;
  }

  /**
   * Process guard variable if present in an if control block.
   * Guards are allowed in any if/else-if block, enabling patterns like:
   * if x <- expr1 with cond1 ... else if y <- expr2 with cond2 ...
   * Each guard variable lives in the chain scope and is accessible to subsequent blocks.
   */
  private GuardVariableDetails processGuardVariableIfPresent(
      final EK9Parser.IfControlBlockContext ctx,
      final String guardScopeId) {

    final var preFlowCtx = ctx.preFlowAndControl();
    if (preFlowCtx == null || preFlowCtx.preFlowStatement() == null) {
      return GuardVariableDetails.none();
    }

    // Process the guard variable declaration and implicit isSet check
    return processGuardVariable(preFlowCtx.preFlowStatement(), guardScopeId);
  }

  /**
   * Process a guard variable from a preFlowStatement.
   * Supports three types:
   * - Variable declaration: if value <- getOptionalValue()  (<- operator, NEEDS guard check)
   * - Assignment statement: if value := getOptionalValue()  (:= operator, NO guard check)
   * - Assignment if unset: if value :=? getOptionalValue()  (:=? operator, NEEDS different pattern)
   * - Guard expression: if value ?= getOptionalValue()      (?= operator, NEEDS guard check)
   * Generates: variable setup + optional guard checks based on operator
   * <p>
   * CRITICAL: Only adds variable to guardVariables list if operator requires guard checks!
   * This determines chain_type: "IF_ELSE_WITH_GUARDS" vs "IF_ELSE_IF"
   * </p>
   */
  private GuardVariableDetails processGuardVariable(
      final EK9Parser.PreFlowStatementContext ctx,
      final String guardScopeId) {

    final var guardSetup = new ArrayList<IRInstr>();
    final var guardVariables = new ArrayList<String>();

    // Handle variable declaration as guard (e.g., value <- expr)
    if (ctx.variableDeclaration() != null) {
      // Generate variable declaration IR (REFERENCE + assignment)
      guardSetup.addAll(generators.variableDeclGenerator.apply(ctx.variableDeclaration()));

      // <- always requires guard check, so add to guardVariables
      final var guardSymbol = getRecordedSymbolOrException(ctx.variableDeclaration());
      final var variableName = new org.ek9lang.compiler.phase7.support.VariableNameForIR().apply(guardSymbol);
      guardVariables.add(variableName);
    } else if (ctx.assignmentStatement() != null) {
      // Generate assignment statement IR
      guardSetup.addAll(generators.assignmentStmtGenerator.apply(ctx.assignmentStatement()));

      // Only add to guardVariables if the operator requires a guard check
      final var operatorType = guardedConditionEvaluator.getGuardOperatorType(ctx);
      if (guardedConditionEvaluator.requiresGuardCheck(operatorType)) {
        final var assignSymbol = getRecordedSymbolOrException(ctx.assignmentStatement().identifier());
        final var variableName = new org.ek9lang.compiler.phase7.support.VariableNameForIR().apply(assignSymbol);
        guardVariables.add(variableName);
      }
      // For := (blind assignment), we do NOT add to guardVariables
      // This ensures chain_type will be "IF_ELSE_IF" not "IF_ELSE_WITH_GUARDS"
    } else if (ctx.guardExpression() != null) {
      // ?= (guarded assignment) checks the RIGHT side (expression result) before assigning
      // For now, treat it similarly to variable declaration with guard semantics
      // The guard check will verify the assigned value is set

      final var guardExpr = ctx.guardExpression();
      final var debugInfo = stackContext.createDebugInfo(guardExpr);

      // Get target variable symbol
      final var targetSymbol = getRecordedSymbolOrException(guardExpr.identifier());
      final var targetName = new org.ek9lang.compiler.phase7.support.VariableNameForIR().apply(targetSymbol);

      // Create temp variable for expression result
      final var tempDetails = createTempVariable(debugInfo);

      // Evaluate the expression (RHS) into temp variable
      guardSetup.addAll(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new org.ek9lang.compiler.phase7.support.ExprProcessingDetails(guardExpr.expression(), tempDetails)
              ),
              tempDetails
          )
      );

      // Assign temp to target variable (similar to := operator)
      // The guard check on the target variable happens in condition evaluation
      guardSetup.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.release(targetName, debugInfo));
      guardSetup.add(
          org.ek9lang.compiler.ir.instructions.MemoryInstr.store(targetName, tempDetails.resultVariable(), debugInfo));
      guardSetup.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.retain(targetName, debugInfo));

      // Add to guardVariables since ?= REQUIRES guard check
      guardVariables.add(targetName);
    } else {
      throw new org.ek9lang.core.CompilerException(
          "Invalid preFlowStatement - expected variableDeclaration, assignmentStatement, or guardExpression");
    }

    return GuardVariableDetails.create(guardVariables, guardSetup, guardScopeId, null);
  }

  /**
   * Process an if control block with its own branch scope.
   * Condition evaluation happens in a TIGHT condition scope (freed immediately).
   * Body evaluation happens in a new nested scope.
   *
   * @param ctx        The if control block context
   * @param blockGuard Guard details for THIS specific block (not all blocks)
   */
  private ConditionCaseDetails processIfControlBlockWithBranchScope(
      final EK9Parser.IfControlBlockContext ctx,
      final GuardVariableDetails blockGuard) {
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Create TIGHT scope just for condition evaluation
    // This scope exits immediately after producing primitive boolean, freeing temps
    final var conditionScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(conditionScopeId, debugInfo, IRFrameType.BLOCK);

    final var conditionEvaluation = new ArrayList<IRInstr>();

    // Enter condition scope
    conditionEvaluation.add(ScopeInstr.enter(conditionScopeId, debugInfo));

    // Determine the condition expression to evaluate
    // If there's a guard but no explicit expression, use implicit isSet check
    final var preFlowCtx = ctx.preFlowAndControl();
    final var conditionDetails = createTempVariable(debugInfo);

    // Evaluate guarded condition using common evaluator (handles all 4 cases)
    // Note: IF statements have additional blockGuard validation, but the core
    // evaluation logic is the same as WHILE/DO-WHILE loops
    final var evaluation = guardedConditionEvaluator.evaluate(
        preFlowCtx.expression(),        // Condition expression (may be null for guard-only)
        preFlowCtx.preFlowStatement(),  // Guard variable (may be null for condition-only)
        conditionDetails,               // Result variable
        conditionScopeId,               // Scope ID for condition evaluation
        debugInfo);                     // Debug information

    // Add the generated condition evaluation instructions
    conditionEvaluation.addAll(evaluation.instructions());
    final var primitiveCondition = evaluation.primitiveCondition();

    // Exit condition scope immediately - frees all condition temps NOW
    conditionEvaluation.add(ScopeInstr.exit(conditionScopeId, debugInfo));
    stackContext.exitScope();

    // Enter NEW scope for this branch body
    final var branchScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(branchScopeId, debugInfo, IRFrameType.BLOCK);

    // Wrap body evaluation with SCOPE_ENTER/EXIT
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(branchScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.block().instructionBlock(), generators.blockStmtGenerator));
    bodyEvaluation.add(ScopeInstr.exit(branchScopeId, debugInfo));

    // Exit branch scope
    stackContext.exitScope();

    // Create condition case with branch scope id
    return ConditionCaseDetails.createExpression(
        branchScopeId,  // Use branch scope id, not chain scope id
        conditionEvaluation,
        conditionDetails.resultVariable(),    // EK9 Boolean result for memory management
        primitiveCondition, // primitive boolean for backend branching
        bodyEvaluation,
        null // No result for statement form
    );
  }

  /**
   * Process an else block with its own branch scope.
   * Else body evaluation happens in a new nested scope.
   */
  private List<IRInstr> processElseBlockWithBranchScope(final EK9Parser.ElseOnlyBlockContext ctx) {
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter NEW scope for else body
    final var elseScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(elseScopeId, debugInfo, IRFrameType.BLOCK);

    // Wrap body evaluation with SCOPE_ENTER/EXIT
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(elseScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.block().instructionBlock(), generators.blockStmtGenerator));
    bodyEvaluation.add(ScopeInstr.exit(elseScopeId, debugInfo));

    // Exit else scope
    stackContext.exitScope();

    return bodyEvaluation;
  }

}