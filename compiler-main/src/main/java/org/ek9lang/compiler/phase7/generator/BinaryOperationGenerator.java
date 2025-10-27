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

    // Get method name from operator map
    final var methodName = operatorMap.getForward(details.ctx().op.getText());

    // Binary operation: left._method(right) -> result
    final var leftExpr = ctx.left != null ? ctx.left : ctx.expression().get(0);
    final var rightExpr = ctx.right != null ? ctx.right : ctx.expression().get(1);

    // Process left operand with memory management
    // Create temp variable with debug info from left operand context (for accurate position tracking)
    final var leftDetails = createTempVariableFromContext(leftExpr);
    final var leftEvaluation = processOperandExpression(leftExpr, leftDetails);
    final var instructions =
        new ArrayList<>(generators.variableMemoryManagement.apply(() -> leftEvaluation, leftDetails));

    // Process right operand with memory management
    // Create temp variable with debug info from right operand context (for accurate position tracking)
    final var rightDetails = createTempVariableFromContext(rightExpr);
    final var rightEvaluation = processOperandExpression(rightExpr, rightDetails);
    instructions.addAll(generators.variableMemoryManagement.apply(() -> rightEvaluation, rightDetails));

    // Get operand symbols for method resolution
    final var leftSymbol = getRecordedSymbolOrException(leftExpr);
    final var rightSymbol = getRecordedSymbolOrException(rightExpr);

    // Extract return type from the binary operation's CallSymbol using helper method
    final var returnType = extractReturnType(ctx, leftSymbol);

    // Create call context for cost-based resolution with parse context
    // Pass parse context so CallDetailsBuilder can access the resolved CallSymbol from Phase 3
    // IMPORTANT: Must pass TYPES, not variable symbols, for promotion to work correctly
    final var callContext = CallContext.forBinaryOperationWithContext(
        leftSymbol.getType().orElseThrow(),          // Target type (not variable symbol)
        rightSymbol.getType().orElseThrow(),         // Argument type (not variable symbol)
        returnType,                                  // Return type (from resolved method)
        methodName,                                  // Method name (from operator map)
        leftDetails.resultVariable(),                // Target variable (left operand variable)
        rightDetails.resultVariable(),               // Argument variable (right operand variable)
        stackContext.currentScopeId(),               // STACK-BASED: Get scope ID from current stack frame
        ctx                                          // Parse context to access CallSymbol with resolved method
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