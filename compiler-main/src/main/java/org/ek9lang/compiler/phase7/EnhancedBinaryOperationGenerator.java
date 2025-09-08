package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.CallContext;
import org.ek9lang.compiler.phase7.support.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Enhanced Binary Operation Generator that demonstrates integration with CallDetailsBuilder.
 * This shows how the new cost-based method resolution and promotion system can be used
 * in place of the manual CallDetails construction in BinaryOperationGenerator.
 * <p>
 * Key improvements:<br>
 * - Uses CallDetailsBuilder for cost-based method resolution<br>
 * - Automatic parameter-by-parameter promotion checking<br>
 * - Composable and reusable across all operation types<br>
 * - Integration with escape analysis metadata system<br>
 * </p>
 */
abstract class EnhancedBinaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();
  private final CallDetailsBuilder callDetailsBuilder;

  EnhancedBinaryOperationGenerator(final IRContext context) {
    super(context);
    this.callDetailsBuilder = new CallDetailsBuilder(context);
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {

    final var ctx = details.ctx();
    final var resultVariable = details.variableDetails().resultVariable();
    final var basicDetails = details.variableDetails().basicDetails();

    // Get method name from operator map
    final var methodName = operatorMap.getForward(details.ctx().op.getText());

    // Binary operation: left._method(right) -> result
    final var leftExpr = ctx.left != null ? ctx.left : ctx.expression().get(0);
    final var rightExpr = ctx.right != null ? ctx.right : ctx.expression().get(1);

    // Process left operand with memory management
    final var leftTemp = context.generateTempName();
    final var leftDetails = new VariableDetails(leftTemp, basicDetails);
    final var leftEvaluation = processOperandExpression(leftExpr, leftDetails);
    final var instructions = new ArrayList<>(variableMemoryManagement.apply(() -> leftEvaluation, leftDetails));

    // Process right operand with memory management
    final var rightTemp = context.generateTempName();
    final var rightDetails = new VariableDetails(rightTemp, basicDetails);
    final var rightEvaluation = processOperandExpression(rightExpr, rightDetails);
    instructions.addAll(variableMemoryManagement.apply(() -> rightEvaluation, rightDetails));

    // Get operand symbols for method resolution
    final var leftSymbol = getRecordedSymbolOrException(leftExpr);
    final var rightSymbol = getRecordedSymbolOrException(rightExpr);

    // Create call context for cost-based resolution
    final var callContext = CallContext.forBinaryOperation(
        symbolTypeOrException.apply(leftSymbol),    // Target type (left operand type)
        symbolTypeOrException.apply(rightSymbol),   // Argument type (right operand type) 
        methodName,                                  // Method name (from operator map)
        leftTemp,                                   // Target variable (left operand variable)
        rightTemp,                                  // Argument variable (right operand variable)
        basicDetails.scopeId()                     // Scope ID
    );

    // Use CallDetailsBuilder for cost-based method resolution and promotion
    final var callDetailsResult = callDetailsBuilder.apply(callContext);

    // Add any promotion instructions that were generated
    instructions.addAll(callDetailsResult.allInstructions());

    // Generate the operator call with resolved CallDetails
    final var debugInfo = debugInfoCreator.apply(new Ek9Token(ctx.op));
    instructions.add(CallInstr.operator(resultVariable, debugInfo, callDetailsResult.callDetails()));

    return instructions;
  }

  /**
   * Process operand expression - this should be overridden by subclasses to provide actual expression processing.
   * Default implementation returns empty list.
   */
  protected abstract List<IRInstr> processOperandExpression(
      final org.ek9lang.antlr.EK9Parser.ExpressionContext operandExpr,
      final VariableDetails operandDetails);
}