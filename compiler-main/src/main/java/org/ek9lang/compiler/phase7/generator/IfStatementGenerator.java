package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
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

    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var scopeId = stackContext.currentScopeId();

    // Process all if/else if conditions into ConditionCaseDetails
    final var conditionChain = new ArrayList<ConditionCaseDetails>();

    for (var ifControlBlock : ctx.ifControlBlock()) {
      conditionChain.add(processIfControlBlock(ifControlBlock));
    }

    // Process else block if present
    List<IRInstr> defaultBodyEvaluation = List.of();
    String defaultResult = null;
    if (ctx.elseOnlyBlock() != null) {
      defaultBodyEvaluation = processElseOnlyBlock(ctx.elseOnlyBlock());
    }

    // Create CONTROL_FLOW_CHAIN details
    final var details = ControlFlowChainDetails.createIfElse(
        null, // No result for statement form
        conditionChain,
        defaultBodyEvaluation,
        defaultResult, // No default result for statement form
        debugInfo,
        scopeId
    );

    // Use ControlFlowChainGenerator to generate IR
    return generators.controlFlowChainGenerator.apply(details);
  }

  private ConditionCaseDetails processIfControlBlock(final EK9Parser.IfControlBlockContext ctx) {
    final var debugInfo = stackContext.createDebugInfo(ctx);
    final var scopeId = stackContext.currentScopeId();

    // Check for preFlowAndControl (guard variables)
    if (ctx.preFlowAndControl() != null && ctx.preFlowAndControl().preFlowStatement() != null) {
      // TODO: Handle guard variables - future enhancement
      throw new CompilerException("Guard variables in if not yet implemented");
    }

    // Process condition expression to get EK9 Boolean
    final var conditionDetails = createTempVariable(debugInfo);
    final var conditionEvaluation = new ArrayList<>(
        generators.exprGenerator.apply(
            new ExprProcessingDetails(ctx.preFlowAndControl().expression(), conditionDetails)
        )
    );

    // Add primitive boolean conversion for backend optimization
    final var conversion = convertToPrimitiveBoolean(conditionDetails.resultVariable(), debugInfo);
    final var primitiveCondition = conversion.addToInstructions(conditionEvaluation);

    // Process body block - access through instructionBlock
    final var bodyEvaluation = processBlockStatements(ctx.block());

    // Create condition case with both EK9 Boolean and primitive boolean
    return ConditionCaseDetails.createExpression(
        scopeId,
        conditionEvaluation,
        conditionDetails.resultVariable(),    // EK9 Boolean result for memory management
        primitiveCondition, // primitive boolean for backend branching
        bodyEvaluation,
        null // No result for statement form
    );
  }

  private List<IRInstr> processElseOnlyBlock(final EK9Parser.ElseOnlyBlockContext ctx) {
    return processBlockStatements(ctx.block());
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