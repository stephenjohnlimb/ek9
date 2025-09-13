package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.LogicalDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallDetailsForIsTrue;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;

/**
 * Both AND/OR are very similar in processing, the only real differences are:<br>
 * What operation to call and the op code to generate.
 */
abstract class AbstractShortCircuitGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final RecordExprProcessing recordExprProcessing;
  private final CallDetailsForIsTrue callDetailsForTrue = new CallDetailsForIsTrue();
  private final VariableMemoryManagement variableMemoryManagement;
  private final Function<LogicalDetails, IRInstr> logicalOperation;

  AbstractShortCircuitGenerator(final IRGenerationContext stackContext,
                                final RecordExprProcessing recordExprProcessing,
                                final Function<LogicalDetails, IRInstr> logicalOperation) {
    super(stackContext);
    this.recordExprProcessing = recordExprProcessing;
    this.logicalOperation = logicalOperation;
    this.variableMemoryManagement = new VariableMemoryManagement(stackContext);
  }

  protected abstract CallDetails getCallDetails(final String lhsVariable, final String rhsVariable);

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var exprResult = details.variableDetails().resultVariable();
    // STACK-BASED: Get scope ID from current stack frame instead of details chain
    final var scopeId = stackContext.currentScopeId();

    // Get debug information
    final var exprSymbol = getRecordedSymbolOrException(ctx);
    final var debugInfo = debugInfoCreator.apply(exprSymbol.getSourceToken());
    final var basicDetails = new BasicDetails(debugInfo);

    final var lhsTemp = stackContext.generateTempName();
    final var lhsVariableDetails = new VariableDetails(lhsTemp, basicDetails);
    final var leftEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.left, lhsVariableDetails)));

    // Convert left operand to primitive boolean condition, so no need for memory management with primitives.
    final var lhsPrimitive = stackContext.generateTempName();
    final var lhsCallDetails = callDetailsForTrue.apply(lhsTemp);
    leftEvaluationInstructions.add(CallInstr.call(lhsPrimitive, debugInfo, lhsCallDetails));

    // Right operand evaluation instructions (for non-short-circuit pathway)
    final var rhsTemp = stackContext.generateTempName();
    final var rhsVariableDetails = new VariableDetails(rhsTemp, basicDetails);
    final var rightEvaluationInstructions = new ArrayList<>(
        recordExprProcessing.apply(new ExprProcessingDetails(ctx.right, rhsVariableDetails)));

    final var result = stackContext.generateTempName();
    final var resultComputationInstructions = new ArrayList<IRInstr>();
    final var callDetails = getCallDetails(lhsTemp, rhsTemp);
    resultComputationInstructions.add(CallInstr.operator(result, debugInfo, callDetails));

    final var variableDetails = new VariableDetails(result, basicDetails);
    variableMemoryManagement.apply(() -> resultComputationInstructions, variableDetails);

    // Create record components for structured data
    final var leftEvaluation = new OperandEvaluation(leftEvaluationInstructions, lhsTemp);
    final var conditionalEvaluation = new ConditionalEvaluation(List.of(), lhsPrimitive);
    final var rightEvaluation = new OperandEvaluation(rightEvaluationInstructions, rhsTemp);
    final var resultEvaluation = new OperandEvaluation(resultComputationInstructions, result);

    final var operation = logicalOperation.apply(
        new LogicalDetails(
            exprResult,
            leftEvaluation,
            conditionalEvaluation,
            rightEvaluation,
            resultEvaluation,
            debugInfo,
            scopeId)
    );

    final var instructions = new ArrayList<IRInstr>();
    instructions.add(operation);

    return instructions;
  }
}