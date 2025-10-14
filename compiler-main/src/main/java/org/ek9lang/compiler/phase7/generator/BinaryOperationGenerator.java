package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;

/**
 * Generates IR instructions for binary operations (e.g., addition, subtraction, comparison, etc.).
 * <p>
 * Handles:<br>
 * - Left and right operand evaluation with proper memory management<br>
 * - Cost-based method resolution with automatic promotion<br>
 * - CallInstr.operator generation for binary method calls<br>
 * </p>
 * <p>
 * PHASE 7 REFACTORING: Consolidated WithProcessor variant into single class.
 * Now accepts optional expressionProcessor for recursive expression handling.
 * </p>
 */
public final class BinaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final GeneratorSet generators;

  BinaryOperationGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
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
    final var leftDetails = createTempVariable(debugInfo);
    final var leftEvaluation = processOperandExpression(leftExpr, leftDetails);
    final var instructions =
        new ArrayList<>(generators.variableMemoryManagement.apply(() -> leftEvaluation, leftDetails));

    // Process right operand with memory management
    final var rightDetails = createTempVariable(debugInfo);
    final var rightEvaluation = processOperandExpression(rightExpr, rightDetails);
    instructions.addAll(generators.variableMemoryManagement.apply(() -> rightEvaluation, rightDetails));

    // Get operand symbols for method resolution
    final var leftSymbol = getRecordedSymbolOrException(leftExpr);
    final var rightSymbol = getRecordedSymbolOrException(rightExpr);

    // Get the resolved method's return type from the binary operation's CallSymbol
    final var exprSymbol = getRecordedSymbolOrException(ctx);
    final ISymbol returnType;
    if (exprSymbol instanceof CallSymbol cs) {
      final var resolvedMethod = cs.getResolvedSymbolToCall();
      switch (resolvedMethod) {
        case MethodSymbol ms -> returnType = ms.getReturningSymbol().getType().orElse(leftSymbol);
        case FunctionSymbol fs -> returnType = fs.getReturningSymbol().getType().orElse(leftSymbol);
        default -> returnType = leftSymbol;  // Fallback for other symbol types
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
        leftDetails.resultVariable(),                // Target variable (left operand variable)
        rightDetails.resultVariable(),               // Argument variable (right operand variable)
        stackContext.currentScopeId()                // STACK-BASED: Get scope ID from current stack frame
    );

    // Use CallDetailsBuilder from generators struct for cost-based method resolution and promotion
    final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

    // Add any promotion instructions that were generated
    instructions.addAll(callDetailsResult.allInstructions());

    // Generate the operator call with resolved CallDetails
    final var opDebugInfo = debugInfoCreator.apply(new Ek9Token(ctx.op));
    instructions.add(CallInstr.operator(resultVariable, opDebugInfo, callDetailsResult.callDetails()));

    return instructions;
  }

  /**
   * Process operand expression using exprGenerator from generators struct.
   * This allows handling complex nested expressions like (a + b) - (c * d).
   */
  List<IRInstr> processOperandExpression(
      final org.ek9lang.antlr.EK9Parser.ExpressionContext operandExpr,
      final VariableDetails operandDetails) {
    return generators.exprGenerator.apply(new ExprProcessingDetails(operandExpr, operandDetails));
  }
}