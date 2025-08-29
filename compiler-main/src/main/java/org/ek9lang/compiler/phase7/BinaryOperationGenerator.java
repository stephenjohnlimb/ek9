package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR instructions for binary operations (e.g., addition, subtraction, comparison, etc.).
 * <p>
 * Handles:
 * - Left and right operand evaluation with proper memory management
 * - Method resolution for correct return type determination
 * - CallInstr.operator generation for binary method calls
 * </p>
 */
abstract class BinaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  BinaryOperationGenerator(final IRContext context) {
    super(context);
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

    // Get operand types and resolve return type
    final var leftSymbol = getRecordedSymbolOrException(leftExpr);
    final var rightSymbol = getRecordedSymbolOrException(rightExpr);

    final var leftType = typeNameOrException.apply(leftSymbol);
    final var rightType = typeNameOrException.apply(rightSymbol);
    //Need to lookup the operator name in ek9 form, not the method we will maps to for the IR.
    final var returnType = resolveBinaryMethodReturnType(leftSymbol, rightSymbol, ctx.op.getText());

    // Create CallDetails for binary operation
    final var callDetails = new CallDetails(leftTemp, leftType, methodName,
        List.of(rightType), returnType, List.of(rightTemp));

    // Generate the operator call
    final var debugInfo = debugInfoCreator.apply(leftSymbol.getSourceToken());
    instructions.add(CallInstr.operator(resultVariable, debugInfo, callDetails));

    return instructions;
  }

  /**
   * Resolve the return type of a binary method by looking up the actual method on the left operand type.
   * Uses the same pattern as RequiredOperatorPresentOrError.
   */
  private String resolveBinaryMethodReturnType(final ISymbol leftSymbol,
                                               final ISymbol rightSymbol,
                                               final String methodName) {

    // Create method search for binary operation (one parameter)
    final var search = new MethodSymbolSearch(methodName);

    search.addTypeParameter(rightSymbol.getType());

    // Get left operand type symbol for method resolution
    final var leftTypeSymbol = leftSymbol.getType().orElse(null);
    if (leftTypeSymbol instanceof AggregateSymbol aggregate) {

      // Resolve method on the left operand type
      final var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      final var bestMatch = results.getSingleBestMatchSymbol();

      if (bestMatch.isPresent()) {
        final var method = bestMatch.get();
        final var returningSymbol = method.getReturningSymbol();
        if (returningSymbol.getType().isPresent()) {
          return typeNameOrException.apply(returningSymbol);
        }
      }
    }
    throw new CompilerException("Must be able to resolve method for binary operator");
  }


  /**
   * Process operand expression - this should be overridden by subclasses to provide actual expression processing.
   * Default implementation returns empty list.
   */
  protected abstract List<IRInstr> processOperandExpression(
      final org.ek9lang.antlr.EK9Parser.ExpressionContext operandExpr,
      final VariableDetails operandDetails);
}