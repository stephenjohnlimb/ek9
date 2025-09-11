package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;

/**
 * Wrapper for BinaryOperationGenerator that injects the recursive expression processor.
 * This allows the generator to handle complex nested expressions like (a + b) - (c * d).
 */
final class BinaryOperationGeneratorWithProcessor extends BinaryOperationGenerator {

  private final Function<ExprProcessingDetails, List<IRInstr>> expressionProcessor;

  BinaryOperationGeneratorWithProcessor(final IRGenerationContext stackContext,
                                        final Function<ExprProcessingDetails, List<IRInstr>> expressionProcessor) {
    super(stackContext);
    this.expressionProcessor = expressionProcessor;
  }

  @Override
  protected List<IRInstr> processOperandExpression(final org.ek9lang.antlr.EK9Parser.ExpressionContext operandExpr,
                                                   final VariableDetails operandDetails) {
    // Use the injected expression processor to handle recursive expression processing
    return expressionProcessor.apply(new ExprProcessingDetails(operandExpr, operandDetails));
  }
}