package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.SymbolTypeOrException;
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
 * Generates IR instructions for unary operations (e.g., unary minus, increment, decrement, etc.).
 * <p>
 * Handles:<br>
 * - Operand evaluation with proper memory management<br>
 * - Cost-based method resolution with automatic promotion<br>
 * - CallInstr.operator generation for unary method calls<br>
 * </p>
 * <p>
 * PHASE 7 REFACTORING: Consolidated WithProcessor variant into single class.
 * Now accepts optional expressionProcessor for recursive expression handling.
 * </p>
 */
public final class UnaryOperationGenerator extends AbstractGenerator
    implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final OperatorMap operatorMap = new OperatorMap();
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
  private final GeneratorSet generators;

  UnaryOperationGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {
    final var ctx = details.ctx();
    final var resultVariable = details.variableDetails().resultVariable();
    final var debugInfo = details.variableDetails().debugInfo();

    // Get method name from operator map, but for tilde need to map to unary -.
    //Also we need to convert the 'not' textual key word to a ~ for not as well.
    final var opText = ctx.op.getText();
    final var lookupOp = opText.equals("-") || opText.equals("not") ? "~" : opText;
    final var methodName = operatorMap.getForward(lookupOp);

    final var operandExpr = ctx.expression().getFirst();

    // Get operand variable - process the expression first with memory management
    final var operandDetails = createTempVariable(debugInfo);

    // Use recursive expression processing to handle the operand (e.g., for -(-x))
    // Access from generators struct
    final var operandEvaluation = processOperandExpression(operandExpr, operandDetails);
    final var instructions =
        new ArrayList<>(generators.variableMemoryManagement.apply(() -> operandEvaluation, operandDetails));

    // Get operand symbol for method resolution
    final var operandSymbol = getRecordedSymbolOrException(operandExpr);

    // Get the resolved method's return type from the unary operation's CallSymbol
    final var exprSymbol = getRecordedSymbolOrException(ctx);
    final ISymbol returnType;
    if (exprSymbol instanceof CallSymbol cs) {
      final var resolvedMethod = cs.getResolvedSymbolToCall();
      switch (resolvedMethod) {
        case MethodSymbol ms -> returnType = ms.getReturningSymbol().getType().orElse(operandSymbol);
        case FunctionSymbol fs -> returnType = fs.getReturningSymbol().getType().orElse(operandSymbol);
        default -> returnType = operandSymbol;  // Fallback for other symbol types
      }
    } else {
      returnType = exprSymbol.getType().orElseThrow();
    }

    // Create call context for cost-based resolution (unary operation has no arguments)
    final var callContext =
        CallContext.forUnaryOperation(symbolTypeOrException.apply(operandSymbol),  // Target type (operand type)
            methodName,                                   // Method name (from operator map)
            operandDetails.resultVariable(),              // Target variable (operand variable)
            returnType,                                   // Return type (from resolved method)
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
   * This allows handling complex nested expressions like -(-x) or -(a + b).
   */
  List<IRInstr> processOperandExpression(final EK9Parser.ExpressionContext operandExpr,
                                         final VariableDetails operandDetails) {
    return generators.exprGenerator.apply(new ExprProcessingDetails(operandExpr, operandDetails));
  }
}