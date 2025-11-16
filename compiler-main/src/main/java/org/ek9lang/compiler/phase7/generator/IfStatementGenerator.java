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

    final var instructions = new ArrayList<IRInstr>();
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Enter NEW scope for entire if/else chain
    // This scope will contain guard variables and condition temporaries
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

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
      if (blockGuard.hasGuardVariables()) {
        allGuardVariables.addAll(blockGuard.guardVariables());
        allGuardSetup.addAll(blockGuard.guardScopeSetup());
      }

      // Process this block's condition with its specific guard
      conditionChain.add(processIfControlBlockWithBranchScope(ifControlBlock, blockGuard));
    }

    // Create combined guard details from all blocks
    final var guardDetails = allGuardVariables.isEmpty()
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
    instructions.addAll(generators.controlFlowChainGenerator.apply(details));

    // Exit chain scope
    instructions.add(ScopeInstr.exit(chainScopeId, debugInfo));
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
   * For variable declarations: if value <- getOptionalValue()
   * Generates: variable declaration + isSet check
   */
  private GuardVariableDetails processGuardVariable(
      final EK9Parser.PreFlowStatementContext ctx,
      final String guardScopeId) {

    final var guardSetup = new ArrayList<IRInstr>();
    final var guardVariables = new ArrayList<String>();

    // Currently only support variable declaration as guard (e.g., value <- expr)
    if (ctx.variableDeclaration() != null) {
      // Generate variable declaration IR (REFERENCE + assignment)
      guardSetup.addAll(generators.variableDeclGenerator.apply(ctx.variableDeclaration()));

      // Get the declared variable name for tracking
      final var guardSymbol = getRecordedSymbolOrException(ctx.variableDeclaration());
      final var variableName = new org.ek9lang.compiler.phase7.support.VariableNameForIR().apply(guardSymbol);
      guardVariables.add(variableName);
    } else {
      throw new org.ek9lang.core.CompilerException(
          "Only variable declarations are currently supported as guards (e.g., if value <- expr)");
    }

    return GuardVariableDetails.create(guardVariables, guardSetup, guardScopeId, null);
  }

  /**
   * Process an if control block with its own branch scope.
   * Condition evaluation happens in a TIGHT condition scope (freed immediately).
   * Body evaluation happens in a new nested scope.
   *
   * @param ctx            The if control block context
   * @param blockGuard     Guard details for THIS specific block (not all blocks)
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

    // Case 1: Guard WITH explicit condition - need BOTH isSet AND condition
    if (blockGuard.hasGuardVariables() && preFlowCtx.preFlowStatement() != null && conditionExpr != null) {
      // Get the guard variable info
      final var guardVarSymbol = getRecordedSymbolOrException(preFlowCtx.preFlowStatement().variableDeclaration());
      final var guardVarName = new org.ek9lang.compiler.phase7.support.VariableNameForIR().apply(guardVarSymbol);

      // Generate isSet check: guard?
      final var operandVar = stackContext.generateTempName();
      conditionEvaluation.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.load(
          operandVar, guardVarName, debugInfo));

      final var booleanType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
      final var guardType = guardVarSymbol.getType().orElseThrow(
          () -> new org.ek9lang.core.CompilerException("Guard variable must have a type"));

      final var isSetTemp = stackContext.generateTempName();
      final var unaryParams = new org.ek9lang.compiler.phase7.support.UnaryOperatorParams(
          operandVar, "?", guardType, booleanType, isSetTemp, stackContext.currentScopeId(), debugInfo);
      conditionEvaluation.addAll(generators.unaryOperatorInvoker.apply(unaryParams));

      // Generate explicit condition evaluation
      final var explicitCondTemp = stackContext.generateTempName();
      final var explicitCondDetails = new org.ek9lang.compiler.phase7.support.VariableDetails(explicitCondTemp, debugInfo);
      conditionEvaluation.addAll(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new ExprProcessingDetails(conditionExpr, explicitCondDetails)
              ),
              explicitCondDetails
          )
      );

      // AND them together: isSet AND condition
      final var isSetOperand = stackContext.generateTempName();
      final var explicitOperand = stackContext.generateTempName();
      conditionEvaluation.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.load(
          isSetOperand, isSetTemp, debugInfo));
      conditionEvaluation.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.load(
          explicitOperand, explicitCondTemp, debugInfo));

      final var binaryParams = new org.ek9lang.compiler.phase7.support.BinaryOperatorParams(
          isSetOperand, explicitOperand, "and",
          booleanType, booleanType, booleanType,
          conditionDetails.resultVariable(), stackContext.currentScopeId(), debugInfo);
      conditionEvaluation.addAll(generators.binaryOperatorInvoker.apply(binaryParams));

    } else if (conditionExpr != null) {
      // Case 2: Explicit condition only (no guard)
      conditionEvaluation.addAll(
          generators.variableMemoryManagement.apply(
              () -> generators.exprGenerator.apply(
                  new ExprProcessingDetails(conditionExpr, conditionDetails)
              ),
              conditionDetails
          )
      );
    } else if (blockGuard.hasGuardVariables() && preFlowCtx.preFlowStatement() != null) {
      // Case 3: Guard only (implicit isSet check)
      final var guardVarSymbol = getRecordedSymbolOrException(preFlowCtx.preFlowStatement().variableDeclaration());
      final var guardVarName = new org.ek9lang.compiler.phase7.support.VariableNameForIR().apply(guardVarSymbol);

      final var operandVar = stackContext.generateTempName();
      conditionEvaluation.add(org.ek9lang.compiler.ir.instructions.MemoryInstr.load(
          operandVar, guardVarName, debugInfo));

      final var booleanType = stackContext.getParsedModule().getEk9Types().ek9Boolean();
      final var guardType = guardVarSymbol.getType().orElseThrow(
          () -> new org.ek9lang.core.CompilerException("Guard variable must have a type"));
      final var unaryParams = new org.ek9lang.compiler.phase7.support.UnaryOperatorParams(
          operandVar, "?", guardType, booleanType,
          conditionDetails.resultVariable(), stackContext.currentScopeId(), debugInfo);
      conditionEvaluation.addAll(generators.unaryOperatorInvoker.apply(unaryParams));
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

}