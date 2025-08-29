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
 * Generates IR instructions for unary operations (e.g., unary minus, increment, decrement, etc.).
 * <p>
 * Handles:
 * - Operand evaluation with proper memory management
 * - Method resolution for correct return type determination
 * - CallInstr.operator generation for unary method calls
 * </p>
 */
class UnaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  UnaryOperationGenerator(final IRContext context) {
    super(context);
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

    // Get operand type and resolve return type
    final var operandSymbol = getRecordedSymbolOrException(operandExpr);
    final var operandType = typeNameOrException.apply(operandSymbol);
    //We must use the operator used in the ek9 code.
    final var returnType = resolveUnaryMethodReturnType(ctx.op.getText(), operandSymbol);

    // Create CallDetails for unary operation
    final var callDetails = new CallDetails(operandTemp, operandType, methodName,
        List.of(), returnType, List.of());

    // Generate the operator call
    final var debugInfo = debugInfoCreator.apply(operandSymbol.getSourceToken());
    instructions.add(CallInstr.operator(resultVariable, debugInfo, callDetails));

    return instructions;
  }

  /**
   * Resolve the return type of a unary method by looking up the actual method on the target type.
   * Uses the same pattern as RequiredOperatorPresentOrError.
   */
  private String resolveUnaryMethodReturnType(final String methodName,
                                              final ISymbol operandSymbol) {
    // Create method search for unary operation (no parameters)
    final var search = new MethodSymbolSearch(methodName);

    // Get operand type symbol for method resolution
    final var operandTypeSymbol = operandSymbol.getType().orElse(null);
    if (operandTypeSymbol instanceof AggregateSymbol aggregate) {

      // Resolve method on the operand type
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

    throw new CompilerException("Must be able to resolve method for unary operator");
  }

  /**
   * Process operand expression - this should be overridden by subclasses to provide actual expression processing.
   * Default implementation returns empty list.
   */
  protected List<IRInstr> processOperandExpression(final org.ek9lang.antlr.EK9Parser.ExpressionContext operandExpr,
                                                   final VariableDetails operandDetails) {
    // Default implementation - should be overridden by wrapper classes
    return new ArrayList<>();
  }
}