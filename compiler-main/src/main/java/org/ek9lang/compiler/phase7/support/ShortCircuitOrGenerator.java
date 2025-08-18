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
 * Generates IR instructions for Boolean OR operations using LOGICAL_OR_BLOCK pattern.
 * <p>
 * This generator creates a declarative logical operation block containing:
 * 1. Left operand evaluation and primitive boolean condition
 * 2. Right operand evaluation instructions (for non-short-circuit path)
 * 3. Result computation instructions (EK9 Boolean._or() call)
 * 4. All memory management
 * </p>
 * <p>
 * Backends can choose between short-circuit and full evaluation strategies
 * based on the usage context and target-specific optimizations.
 * </p>
 */
public final class ShortCircuitOrGenerator implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final IRContext context;
  private final DebugInfoCreator debugInfoCreator;
  private final RecordExprProcessing recordExprProcessing;
  private final CallDetailsForTrue callDetailsForTrue = new CallDetailsForTrue();

  public ShortCircuitOrGenerator(final IRContext context,
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
    final var debugInfo = debugInfoCreator.apply(exprSymbol);


    // Left operand evaluation
    final var lhsTemp = context.generateTempName();
    final var instructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.left, lhsTemp, scopeId, debugInfo)));

    // Convert left operand to primitive boolean condition
    final var lhsPrimitive = context.generateTempName();
    final var lhsCallDetails = callDetailsForTrue.apply(lhsTemp);
    instructions.add(CallInstr.call(lhsPrimitive, debugInfo, lhsCallDetails));

    // Right operand evaluation instructions (for non-short-circuit pathway)
    final var rhsTemp = context.generateTempName();
    final var rightEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.right, rhsTemp, scopeId, debugInfo)));

    // Note: Memory management already handled by recordExprProcessing

    // Result computation instructions (EK9 Boolean._or() call)
    final var orResult = context.generateTempName();
    final var resultComputationInstructions = new ArrayList<IRInstr>();
    final var orCallDetails = new CallDetails(lhsTemp, "org.ek9.lang::Boolean",
        "_or", List.of("org.ek9.lang::Boolean"), "org.ek9.lang::Boolean", List.of(rhsTemp));
    resultComputationInstructions.add(CallInstr.operator(orResult, debugInfo, orCallDetails));

    // Memory management for logical result
    resultComputationInstructions.add(MemoryInstr.retain(orResult, debugInfo));
    resultComputationInstructions.add(ScopeInstr.register(orResult, scopeId, debugInfo));

    // Create logical OR operation block
    final var logicalOperation = LogicalOperationInstr.orOperation(
        exprResult,
        lhsTemp,
        lhsPrimitive,
        rightEvaluationInstructions,
        rhsTemp,
        resultComputationInstructions,
        orResult,
        scopeId,
        debugInfo
    );
    instructions.add(logicalOperation);

    return instructions;
  }
}