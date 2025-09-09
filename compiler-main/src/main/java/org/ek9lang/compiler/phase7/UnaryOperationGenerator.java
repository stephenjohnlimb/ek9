package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
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
 * Generates IR instructions for unary operations (e.g., unary minus, increment, decrement, etc.).
 * <p>
 * Handles:<br>
 * - Operand evaluation with proper memory management<br>
 * - Cost-based method resolution with automatic promotion<br>
 * - CallInstr.operator generation for unary method calls<br>
 * </p>
 */
abstract class UnaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();
  private final CallDetailsBuilder callDetailsBuilder;

  UnaryOperationGenerator(final IRContext context) {
    super(context);
    this.callDetailsBuilder = new CallDetailsBuilder(context);
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var resultVariable = details.variableDetails().resultVariable();
    final var basicDetails = details.variableDetails().basicDetails();

    // Get method name from operator map, but for tilde need to map to unary -.
    final var lookupOp = ctx.op.getText().equals("-") ? "~" : ctx.op.getText();
    final var methodName = operatorMap.getForward(lookupOp);

    final var operandExpr = ctx.expression().getFirst();

    // Get operand variable - process the expression first with memory management
    final var operandTemp = context.generateTempName();
    final var operandDetails = new VariableDetails(operandTemp, basicDetails);

    // Use recursive expression processing to handle the operand (e.g., for -(-x))
    // This will be injected from ExprInstrGenerator constructor
    final var operandEvaluation = processOperandExpression(operandExpr, operandDetails);
    final var instructions = new ArrayList<>(variableMemoryManagement.apply(() -> operandEvaluation, operandDetails));

    // Get operand symbol for method resolution
    final var operandSymbol = getRecordedSymbolOrException(operandExpr);

    // Create call context for cost-based resolution (unary operation has no arguments)
    final var callContext =
        CallContext.forUnaryOperation(symbolTypeOrException.apply(operandSymbol),  // Target type (operand type)
            methodName,                                   // Method name (from operator map)
            operandTemp,                                  // Target variable (operand variable)
            basicDetails.scopeId()                       // Scope ID
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
  protected abstract List<IRInstr> processOperandExpression(final EK9Parser.ExpressionContext operandExpr,
                                                            final VariableDetails operandDetails);
}