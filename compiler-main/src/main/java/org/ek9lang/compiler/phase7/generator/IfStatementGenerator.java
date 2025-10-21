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
import org.ek9lang.compiler.phase7.support.BooleanExtractionParams;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

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
    // This scope will contain guard variables (future) and condition temporaries
    final var chainScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(chainScopeId, debugInfo, IRFrameType.BLOCK);

    instructions.add(ScopeInstr.enter(chainScopeId, debugInfo));

    // TODO Phase 2: Process guard variable if exists (first block only)
    // For now, keep throwing exception in processIfControlBlockWithBranchScope

    // Process all if/else if conditions with UNIQUE branch scopes
    final var conditionChain = new ArrayList<ConditionCaseDetails>();
    for (var ifControlBlock : ctx.ifControlBlock()) {
      conditionChain.add(processIfControlBlockWithBranchScope(ifControlBlock));
    }

    // Process else block with its OWN scope
    List<IRInstr> defaultBodyEvaluation = List.of();
    if (ctx.elseOnlyBlock() != null) {
      defaultBodyEvaluation = processElseBlockWithBranchScope(ctx.elseOnlyBlock());
    }

    // Create CONTROL_FLOW_CHAIN details (chainScopeId is the outer scope)
    final var details = ControlFlowChainDetails.createIfElse(
        null, // No result for statement form
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
   * Process an if control block with its own branch scope.
   * Condition evaluation happens in the current (chain) scope.
   * Body evaluation happens in a new nested scope.
   */
  private ConditionCaseDetails processIfControlBlockWithBranchScope(final EK9Parser.IfControlBlockContext ctx) {
    final var debugInfo = stackContext.createDebugInfo(ctx);

    // Check for preFlowAndControl (guard variables)
    if (ctx.preFlowAndControl() != null && ctx.preFlowAndControl().preFlowStatement() != null) {
      // TODO Phase 2: Handle guard variables - future enhancement
      throw new CompilerException("Guard variables in if not yet implemented");
    }

    // Process condition expression in CURRENT scope (chain scope)
    // Condition temporaries live in chain scope, accessible for memory management
    final var conditionDetails = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<>(
        generators.exprGenerator.apply(
            new ExprProcessingDetails(ctx.preFlowAndControl().expression(), conditionDetails)
        )
    );

    // Add primitive boolean conversion for backend optimization
    final var primitiveCondition = stackContext.generateTempName();
    final var extractionParams = new BooleanExtractionParams(
        conditionDetails.resultVariable(), primitiveCondition, debugInfo);
    conditionEvaluation.addAll(generators.primitiveBooleanExtractor.apply(extractionParams));

    // Enter NEW scope for this branch body
    final var branchScopeId = stackContext.generateScopeId(IRConstants.GENERAL_SCOPE);
    stackContext.enterScope(branchScopeId, debugInfo, IRFrameType.BLOCK);

    // Wrap body evaluation with SCOPE_ENTER/EXIT
    final var bodyEvaluation = new ArrayList<IRInstr>();
    bodyEvaluation.add(ScopeInstr.enter(branchScopeId, debugInfo));
    bodyEvaluation.addAll(processBlockStatements(ctx.block()));
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
    bodyEvaluation.addAll(processBlockStatements(ctx.block()));
    bodyEvaluation.add(ScopeInstr.exit(elseScopeId, debugInfo));

    // Exit else scope
    stackContext.exitScope();

    return bodyEvaluation;
  }

  /**
   * Process all block statements in a block context.
   * Consolidates the common pattern of iterating through block statements.
   */
  private List<IRInstr> processBlockStatements(final EK9Parser.BlockContext blockCtx) {
    final var instructions = new ArrayList<IRInstr>();
    for (var blockStatement : blockCtx.instructionBlock().blockStatement()) {
      instructions.addAll(generators.blockStmtGenerator.apply(blockStatement));
    }
    return instructions;
  }
}