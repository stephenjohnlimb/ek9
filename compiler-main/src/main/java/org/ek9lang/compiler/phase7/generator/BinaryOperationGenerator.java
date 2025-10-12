package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.calls.CallDetailsBuilder;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Generates IR instructions for binary operations (e.g., addition, subtraction, comparison, etc.).
 * <p>
 * Handles:<br>
 * - Left and right operand evaluation with proper memory management<br>
 * - Cost-based method resolution with automatic promotion<br>
 * - CallInstr.operator generation for binary method calls<br>
 * </p>
 */
abstract class BinaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final VariableMemoryManagement variableMemoryManagement;

  BinaryOperationGenerator(final IRGenerationContext stackContext) {
    super(stackContext);
    this.variableMemoryManagement = new VariableMemoryManagement(stackContext);
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {

    final var ctx = details.ctx();
    final var resultVariable = details.variableDetails().resultVariable();
    final var debugInfo = details.variableDetails().debugInfo();

    // Get method name from operator map
    final var methodName = operatorMap.getForward(details.ctx().op.getText());

    // Binary operation: left._method(right) -> result
    final var leftExpr = ctx.left != null ? ctx.left : ctx.expression().get(0);
    final var rightExpr = ctx.right != null ? ctx.right : ctx.expression().get(1);

    // Process left operand with memory management
    final var leftTemp = stackContext.generateTempName();
    final var leftDetails = new VariableDetails(leftTemp, debugInfo);
    final var leftEvaluation = processOperandExpression(leftExpr, leftDetails);
    final var instructions = new ArrayList<>(variableMemoryManagement.apply(() -> leftEvaluation, leftDetails));

    // Process right operand with memory management
    final var rightTemp = stackContext.generateTempName();
    final var rightDetails = new VariableDetails(rightTemp, debugInfo);
    final var rightEvaluation = processOperandExpression(rightExpr, rightDetails);
    instructions.addAll(variableMemoryManagement.apply(() -> rightEvaluation, rightDetails));

    // Get operand symbols for method resolution
    final var leftSymbol = getRecordedSymbolOrException(leftExpr);
    final var rightSymbol = getRecordedSymbolOrException(rightExpr);

    // Get the resolved method's return type from the binary operation's CallSymbol
    final var exprSymbol = getRecordedSymbolOrException(ctx);
    final ISymbol returnType;
    if (exprSymbol instanceof org.ek9lang.compiler.symbols.CallSymbol cs) {
      final var resolvedMethod = cs.getResolvedSymbolToCall();
      if (resolvedMethod instanceof org.ek9lang.compiler.symbols.MethodSymbol ms) {
        returnType = ms.getReturningSymbol().getType().orElse(leftSymbol);
      } else if (resolvedMethod instanceof org.ek9lang.compiler.symbols.FunctionSymbol fs) {
        returnType = fs.getReturningSymbol().getType().orElse(leftSymbol);
      } else {
        returnType = leftSymbol;  // Fallback for other symbol types
      }
    } else {
      returnType = exprSymbol.getType().orElseThrow();
    }

    // Create call context for cost-based resolution
    final var callContext = CallContext.forBinaryOperation(
        leftSymbol,                                  // Target type (left operand type)
        rightSymbol,                                 // Argument type (right operand type)
        returnType,                                  // Return type (from resolved method)
        methodName,                                  // Method name (from operator map)
        leftTemp,                                    // Target variable (left operand variable)
        rightTemp,                                   // Argument variable (right operand variable)
        stackContext.currentScopeId()                // STACK-BASED: Get scope ID from current stack frame
    );

    // Use CallDetailsBuilder for cost-based method resolution and promotion
    final var callDetailsBuilder = new CallDetailsBuilder(stackContext);
    final var callDetailsResult = callDetailsBuilder.apply(callContext);

    // Add any promotion instructions that were generated
    instructions.addAll(callDetailsResult.allInstructions());

    // Generate the operator call with resolved CallDetails
    final var opDebugInfo = debugInfoCreator.apply(new Ek9Token(ctx.op));
    instructions.add(CallInstr.operator(resultVariable, opDebugInfo, callDetailsResult.callDetails()));

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