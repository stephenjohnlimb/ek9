package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.data.ConditionCaseDetails;
import org.ek9lang.compiler.ir.data.ControlFlowChainDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR for if/else statements using CONTROL_FLOW_CHAIN.
 * Uses GeneratorSet pattern for dependency injection.
 *
 * Transforms EK9 if statements into CONTROL_FLOW_CHAIN instructions that
 * work equally well for JVM (stack-based) and LLVM (SSA-based) backends.
 */
public final class IfStatementGenerator extends AbstractGenerator
    implements Function<EK9Parser.IfStatementContext, List<IRInstr>> {

  private final GeneratorSet generators;
  private final CallDetailsForIsTrue callDetailsForIsTrue = new CallDetailsForIsTrue();

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
    final var conditionResult = stackContext.generateTempName();
    final var conditionDetails = new VariableDetails(conditionResult, debugInfo);
    final var conditionEvaluation = new ArrayList<>(
        generators.exprGenerator.apply(
            new ExprProcessingDetails(ctx.preFlowAndControl().expression(), conditionDetails)
        )
    );

    // Add primitive boolean conversion for backend optimization
    // Call _true() on the Boolean to get primitive boolean
    final var primitiveCondition = stackContext.generateTempName();
    conditionEvaluation.add(CallInstr.operator(
        new VariableDetails(primitiveCondition, debugInfo),
        callDetailsForIsTrue.apply(conditionResult)
    ));

    // Process body block - access through instructionBlock
    final var bodyEvaluation = new ArrayList<IRInstr>();
    for (var blockStatement : ctx.block().instructionBlock().blockStatement()) {
      bodyEvaluation.addAll(generators.blockStmtGenerator.apply(blockStatement));
    }

    // Create condition case with both EK9 Boolean and primitive boolean
    return ConditionCaseDetails.createExpression(
        scopeId,
        conditionEvaluation,
        conditionResult,    // EK9 Boolean result for memory management
        primitiveCondition, // primitive boolean for backend branching
        bodyEvaluation,
        null // No result for statement form
    );
  }

  private List<IRInstr> processElseOnlyBlock(final EK9Parser.ElseOnlyBlockContext ctx) {
    final var instructions = new ArrayList<IRInstr>();
    for (var blockStatement : ctx.block().instructionBlock().blockStatement()) {
      instructions.addAll(generators.blockStmtGenerator.apply(blockStatement));
    }
    return instructions;
  }
}