package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.LogicalOperationInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;

/**
 * Generates IR instructions for Boolean AND operations using LOGICAL_AND_BLOCK pattern.
 * <p>
 * This generator creates a declarative logical operation block containing:
 * 1. Left operand evaluation and primitive boolean condition
 * 2. Right operand evaluation instructions (for non-short-circuit path)
 * 3. Result computation instructions (EK9 Boolean._and() call)
 * 4. All memory management
 * </p>
 * <p>
 * Backends can choose between short-circuit and full evaluation strategies
 * based on the usage context and target-specific optimizations.
 * </p>
 */
public final class ShortCircuitAndGenerator implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  private final RecordExprProcessing recordExprProcessing;
  private final CallDetailsForTrue callDetailsForTrue = new CallDetailsForTrue();

  public ShortCircuitAndGenerator(final IRContext context,
                                  final RecordExprProcessing recordExprProcessing) {
    this.context = context;
    this.debugInfoCreator = new DebugInfoCreator(context);
    this.recordExprProcessing = recordExprProcessing;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.exprResult();
    final var scopeId = details.scopeId();

    // Get debug information
    final var exprSymbol = context.getParsedModule().getRecordedSymbol(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());

    // Left operand evaluation instructions (for LOGICAL_AND_BLOCK)
    final var lhsTemp = context.generateTempName();
    final var leftEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.left, lhsTemp, scopeId, debugInfo)));

    // Convert left operand to primitive boolean condition
    final var lhsPrimitive = context.generateTempName();
    final var lhsCallDetails = callDetailsForTrue.apply(lhsTemp);
    leftEvaluationInstructions.add(CallInstr.call(lhsPrimitive, debugInfo, lhsCallDetails));

    // Right operand evaluation instructions (for non-short-circuit pathway)
    final var rhsTemp = context.generateTempName();
    final var rightEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.right, rhsTemp, scopeId, debugInfo)));

    // Note: Memory management already handled by recordExprProcessing

    // Result computation instructions (EK9 Boolean._and() call)
    final var andResult = context.generateTempName();
    final var resultComputationInstructions = new ArrayList<IRInstr>();
    final var andCallDetails = new CallDetails(lhsTemp, "org.ek9.lang::Boolean",
        "_and", List.of("org.ek9.lang::Boolean"), "org.ek9.lang::Boolean", List.of(rhsTemp));
    resultComputationInstructions.add(CallInstr.operator(andResult, debugInfo, andCallDetails));

    // Memory management for logical result
    resultComputationInstructions.add(MemoryInstr.retain(andResult, debugInfo));
    resultComputationInstructions.add(ScopeInstr.register(andResult, scopeId, debugInfo));

    // Create logical AND operation block with left evaluation instructions
    final var logicalOperation = LogicalOperationInstr.andOperation(
        exprResult,
        leftEvaluationInstructions,
        lhsTemp,
        lhsPrimitive,
        rightEvaluationInstructions,
        rhsTemp,
        resultComputationInstructions,
        andResult,
        scopeId,
        debugInfo
    );

    // Main instructions list only contains the LOGICAL_AND_BLOCK
    final var instructions = new ArrayList<IRInstr>();
    instructions.add(logicalOperation);

    return instructions;
  }
}