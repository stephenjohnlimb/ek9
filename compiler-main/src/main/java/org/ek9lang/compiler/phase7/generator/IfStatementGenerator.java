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
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
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

  public IfStatementGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    AssertValue.checkNotNull("GeneratorSet cannot be null", generators);
    this.generators = generators;
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
      final var operatorType = getGuardOperatorType(ctx);
      if (requiresGuardCheck(operatorType)) {
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
    final var conditionExpr = preFlowCtx.expression();

    final var conditionDetails = createTempVariable(debugInfo);

    // Determine the guard operator type to decide if guard checks are needed
    final var operatorType = getGuardOperatorType(preFlowCtx.preFlowStatement());
    final var needsGuardCheck = requiresGuardCheck(operatorType);

    // Check if there's ANY guard setup (declaration/assignment), regardless of whether it needs checks
    final var hasGuardSetup = preFlowCtx.preFlowStatement() != null
        && !blockGuard.guardScopeSetup().isEmpty();

    // Case 1: Guard WITH explicit condition AND operator requires guard check (e.g., <-, ?=)
    // Use LOGICAL_AND_BLOCK for short-circuit evaluation (condition only evaluated if guard succeeds)
    if (blockGuard.hasGuardVariables() && hasGuardSetup && conditionExpr != null && needsGuardCheck) {
      // Get the guard variable info using helper method (handles all guard types)
      final var guardVarSymbol = getGuardSymbol(preFlowCtx.preFlowStatement());

      final var booleanType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
      final var booleanTypeName = typeNameOrException.apply(booleanType);

      // LEFT EVALUATION: Generate question operator with explicit IS_NULL check
      final var guardCheckTemp = stackContext.generateTempName();
      final var leftEvaluationInstructions = new ArrayList<>(
          generators.controlFlowChainGenerator.generateQuestionOperatorForVariable(
              guardVarSymbol, guardCheckTemp, debugInfo));

      // CONDITIONAL EVALUATION: Convert guard result to primitive boolean for short-circuit check
      final var guardPrimitive = stackContext.generateTempName();
      final var guardTrueCallDetails = new org.ek9lang.compiler.ir.data.CallDetails(
          guardCheckTemp, booleanTypeName, "_true", List.of(), "boolean", List.of(),
          new org.ek9lang.compiler.ir.data.CallMetaDataDetails(true, 0), false);
      leftEvaluationInstructions.add(org.ek9lang.compiler.ir.instructions.CallInstr.call(
          guardPrimitive, debugInfo, guardTrueCallDetails));

      // RIGHT EVALUATION: Generate explicit condition (ONLY evaluated if guard succeeds)
      final var explicitCondTemp = stackContext.generateTempName();
      final var explicitCondDetails =
          new org.ek9lang.compiler.phase7.support.VariableDetails(explicitCondTemp, debugInfo);
      final var rightEvaluationInstructions = new ArrayList<>(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new ExprProcessingDetails(conditionExpr, explicitCondDetails)
              ),
              explicitCondDetails
          )
      );

      // RESULT EVALUATION: AND guard with condition
      final var resultTemp = stackContext.generateTempName();
      final var andCallDetails = new org.ek9lang.compiler.ir.data.CallDetails(
          guardCheckTemp, booleanTypeName, "_and", List.of(booleanTypeName), booleanTypeName,
          List.of(explicitCondTemp), new org.ek9lang.compiler.ir.data.CallMetaDataDetails(true, 0), false);
      final var resultComputationInstructions = new ArrayList<IRInstr>();
      resultComputationInstructions.add(org.ek9lang.compiler.ir.instructions.CallInstr.operator(
          resultTemp, debugInfo, andCallDetails));

      final var resultVariableDetails = new org.ek9lang.compiler.phase7.support.VariableDetails(resultTemp, debugInfo);
      generators.variableMemoryManagement.apply(() -> resultComputationInstructions, resultVariableDetails);

      // Create LOGICAL_AND_BLOCK with short-circuit evaluation
      final var leftEvaluation = new org.ek9lang.compiler.phase7.support.OperandEvaluation(
          leftEvaluationInstructions, guardCheckTemp);
      final var conditionalEvaluation = new org.ek9lang.compiler.phase7.support.ConditionalEvaluation(
          List.of(), guardPrimitive);
      final var rightEvaluation = new org.ek9lang.compiler.phase7.support.OperandEvaluation(
          rightEvaluationInstructions, explicitCondTemp);
      final var resultEvaluation = new org.ek9lang.compiler.phase7.support.OperandEvaluation(
          resultComputationInstructions, resultTemp);

      final var logicalDetails = new org.ek9lang.compiler.ir.data.LogicalDetails(
          conditionDetails.resultVariable(),
          leftEvaluation,
          conditionalEvaluation,
          rightEvaluation,
          resultEvaluation,
          debugInfo,
          conditionScopeId);

      final var logicalAndBlock =
          org.ek9lang.compiler.ir.instructions.LogicalOperationInstr.andOperation(logicalDetails);
      conditionEvaluation.add(logicalAndBlock);

    } else if (hasGuardSetup && conditionExpr != null && !needsGuardCheck) {
      // Case 2: Guard setup (declaration/assignment) with condition but NO guard check required (e.g., :=, =)
      // Just evaluate the explicit condition (guard setup was already included in the chain scope)
      conditionEvaluation.addAll(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new ExprProcessingDetails(conditionExpr, conditionDetails)
              ),
              conditionDetails
          )
      );
    } else if (conditionExpr != null) {
      // Case 3: Explicit condition only (no guard variable at all)
      conditionEvaluation.addAll(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new ExprProcessingDetails(conditionExpr, conditionDetails)
              ),
              conditionDetails
          )
      );
    } else if (hasGuardSetup && blockGuard.hasGuardVariables() && needsGuardCheck) {
      // Case 4: Guard only (implicit isSet check) AND guard check required (e.g., <-, ?=)
      final var guardVarSymbol = getGuardSymbol(preFlowCtx.preFlowStatement());

      // Generate question operator with explicit IS_NULL check
      // This replaces UnaryOperatorParams to emit IS_NULL instruction in IR
      conditionEvaluation.addAll(
          generators.controlFlowChainGenerator.generateQuestionOperatorForVariable(
              guardVarSymbol, conditionDetails.resultVariable(), debugInfo));
    } else {
      throw new org.ek9lang.core.CompilerException(
          "If statement must have either an expression or a guard variable");
    }

    // Add primitive boolean conversion (still in condition scope)
    final var primitiveCondition = stackContext.generateTempName();
    final var extractionParams = new BooleanExtractionParams(
        conditionDetails.resultVariable(), primitiveCondition, debugInfo);
    conditionEvaluation.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));

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

  /**
   * Gets the symbol from a preFlowStatement (handles variableDeclaration, assignmentStatement, guardExpression).
   *
   * @param preFlowStmt The preFlowStatement context
   * @return The recorded symbol for the guard variable
   */
  private org.ek9lang.compiler.symbols.ISymbol getGuardSymbol(final EK9Parser.PreFlowStatementContext preFlowStmt) {
    if (preFlowStmt == null) {
      throw new org.ek9lang.core.CompilerException("preFlowStatement cannot be null");
    }

    if (preFlowStmt.variableDeclaration() != null) {
      return getRecordedSymbolOrException(preFlowStmt.variableDeclaration());
    }

    if (preFlowStmt.assignmentStatement() != null) {
      // Symbol is recorded on the identifier, not the statement
      return getRecordedSymbolOrException(preFlowStmt.assignmentStatement().identifier());
    }

    if (preFlowStmt.guardExpression() != null) {
      // Symbol is recorded on the identifier, not the expression
      return getRecordedSymbolOrException(preFlowStmt.guardExpression().identifier());
    }

    throw new org.ek9lang.core.CompilerException(
        "Invalid preFlowStatement - expected variableDeclaration, assignmentStatement, or guardExpression");
  }

  /**
   * Determines the guard operator type from a preFlowStatement.
   * Returns the operator token type, or null if not a guard pattern.
   *
   * @param preFlowStmt The preFlowStatement context
   * @return The operator token type (LEFT_ARROW, ASSIGN, ASSIGN2, GUARD, ASSIGN_UNSET), or null
   */
  private Integer getGuardOperatorType(final EK9Parser.PreFlowStatementContext preFlowStmt) {
    if (preFlowStmt == null) {
      return null;
    }

    // Check variableDeclaration: <-, :=, =, etc.
    if (preFlowStmt.variableDeclaration() != null) {
      final var varDecl = preFlowStmt.variableDeclaration();
      if (varDecl.op != null) {
        return varDecl.op.getType();
      }
    }

    // Check assignmentStatement: :=, :=?, etc.
    if (preFlowStmt.assignmentStatement() != null) {
      final var assignStmt = preFlowStmt.assignmentStatement();
      if (assignStmt.op != null) {
        return assignStmt.op.getType();
      }
    }

    // Check guardExpression: ?=
    if (preFlowStmt.guardExpression() != null) {
      final var guardExpr = preFlowStmt.guardExpression();
      if (guardExpr.op != null) {
        return guardExpr.op.getType();
      }
    }

    return null;
  }

  /**
   * Checks if the given operator requires guard checks (null check + isSet check).
   * Returns true for operators that need guard checks: <-, ?=
   * Returns false for operators that don't: :=, =
   *
   * @param operatorType The operator token type
   * @return true if guard checks are required, false otherwise
   */
  private boolean requiresGuardCheck(final Integer operatorType) {
    if (operatorType == null) {
      return false;
    }

    // LEFT_ARROW (<-) - Declaration guard - WITH guard check
    if (operatorType == org.ek9lang.antlr.EK9Parser.LEFT_ARROW) {
      return true;
    }

    // GUARD (?=) - Guarded assignment - WITH guard check
    if (operatorType == org.ek9lang.antlr.EK9Parser.GUARD) {
      return true;
    }

    // ASSIGN (:=) or ASSIGN2 (=) - Blind assignment - NO guard check
    if (operatorType == org.ek9lang.antlr.EK9Parser.ASSIGN
        || operatorType == org.ek9lang.antlr.EK9Parser.ASSIGN2) {
      return false;
    }

    // ASSIGN_UNSET (:=?) - Assignment if unset - WITH guard check on result
    // After conditional assignment (only if left was unset), check if result is now set
    return operatorType == org.ek9lang.antlr.EK9Parser.ASSIGN_UNSET;
  }

}