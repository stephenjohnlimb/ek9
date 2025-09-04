package org.ek9lang.compiler.phase7;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR instructions for function/method calls.
 * <p>
 * Handles:
 * - Function calls: functionName(args...)
 * - Method calls: object.methodName(args...)
 * - Parameter evaluation with proper memory management
 * - Return type resolution and metadata extraction
 * - CallInstr.call generation for function invocations
 * </p>
 */
final class CallInstrGenerator extends AbstractGenerator
    implements BiFunction<EK9Parser.CallContext, VariableDetails, List<IRInstr>> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();
  private final ExprInstrGenerator exprGenerator;

  CallInstrGenerator(final IRContext context) {
    super(context);
    this.exprGenerator = new ExprInstrGenerator(context);
  }

  @Override
  public List<IRInstr> apply(final EK9Parser.CallContext ctx, final VariableDetails resultDetails) {
    AssertValue.checkNotNull("CallContext cannot be null", ctx);
    AssertValue.checkNotNull("VariableDetails cannot be null", resultDetails);

    // Handle different types of calls
    if (ctx.identifierReference() != null && ctx.paramExpression() != null) {
      // Function call: functionName(args...)
      return generateFunctionCall(ctx, resultDetails);
    } else if (ctx.primaryReference() != null && ctx.paramExpression() != null) {
      // Method call on primary reference: object.method(args...)
      throw new CompilerException("Primary reference method calls not yet implemented");
    } else if (ctx.parameterisedType() != null) {
      // Constructor call: Type()
      throw new CompilerException("Constructor calls not yet implemented");
    } else if (ctx.LPAREN() != null && ctx.parameterisedType() != null) {
      // Explicit constructor: (Type())
      throw new CompilerException("Explicit constructor calls not yet implemented");
    } else if (ctx.dynamicFunctionDeclaration() != null) {
      // Dynamic function calls
      throw new CompilerException("Dynamic function calls not yet implemented");
    } else if (ctx.call() != null && ctx.paramExpression() != null) {
      // Nested call with parameters: call(args...)
      throw new CompilerException("Nested calls not yet implemented");
    } else {
      throw new CompilerException("Unsupported call type: " + ctx.getText());
    }
  }

  /**
   * Generate IR for function calls: functionName(args...) or variableName(args...)
   */
  private List<IRInstr> generateFunctionCall(final EK9Parser.CallContext ctx,
                                             final VariableDetails resultDetails) {
    final var instructions = new ArrayList<IRInstr>();

    // Get call symbol - all calls go through CallSymbol in Phase 3 resolution
    final var symbol = getRecordedSymbolOrException(ctx);
    if (symbol instanceof CallSymbol callSymbol) {
      final var resolvedSymbol = callSymbol.getResolvedSymbolToCall();

      // Process parameters first (common to both patterns)
      final var paramExpr = ctx.paramExpression();
      final var argumentDetails = processParameters(paramExpr, instructions, resultDetails.basicDetails().scopeId());

      // Resolve return type and metadata (common to both patterns)
      final var returnType = resolveReturnType(resolvedSymbol);
      final var metaData = extractMetaData(resolvedSymbol);

      // Generate different IR based on what we're calling
      if (resolvedSymbol instanceof FunctionSymbol functionSymbol) {
        // Pattern 1: Named function call - need FUNCTION_INSTANCE
        return generateNamedFunctionCall(instructions, functionSymbol, argumentDetails, returnType, metaData,
            resultDetails, ctx);
      } else {
        // For now, treat everything else as named functions until we implement function variables
        // TODO: Implement VariableSymbol handling for function variables
        throw new CompilerException("Function variable calls not yet implemented. Got: "
            + resolvedSymbol.getClass().getSimpleName() + " - " + resolvedSymbol);
      }
    } else {
      throw new CompilerException("Expected CallSymbol, but got: " + symbol.getClass().getSimpleName());
    }
  }

  /**
   * Generate IR for named function calls: aFunction(args...)
   * Uses FUNCTION_INSTANCE to get the singleton instance.
   */
  private List<IRInstr> generateNamedFunctionCall(final List<IRInstr> instructions,
                                                  final FunctionSymbol functionSymbol,
                                                  final ArgumentDetails argumentDetails,
                                                  final String returnType,
                                                  final CallMetaData metaData,
                                                  final VariableDetails resultDetails,
                                                  final EK9Parser.CallContext ctx) {

    final var fullyQualifiedFunctionName = functionSymbol.getFullyQualifiedName();
    final var debugInfo = debugInfoCreator.apply(new Ek9Token(ctx.identifierReference().start));

    // Step 1: Get the singleton instance of the function
    final var functionInstanceVar = context.generateTempName();
    instructions.add(MemoryInstr.functionInstance(functionInstanceVar, fullyQualifiedFunctionName, debugInfo));

    // Step 2: Call _call on the function singleton instance
    // No RETAIN needed - singletons are never released
    final var callDetails = new CallDetails(
        functionInstanceVar, // Target: the singleton instance variable
        fullyQualifiedFunctionName, // Target type: the function type  
        "_call", // Method: always "_call" for functions
        argumentDetails.parameterTypes(),
        returnType,
        argumentDetails.argumentVariables(),
        metaData
    );

    instructions.add(CallInstr.call(resultDetails.resultVariable(), debugInfo, callDetails));
    return instructions;
  }

  /**
   * Process parameter expressions and return argument details.
   */
  private ArgumentDetails processParameters(final EK9Parser.ParamExpressionContext paramExpr,
                                            final List<IRInstr> instructions,
                                            final String scopeId) {

    final var argumentVariables = new ArrayList<String>();
    final var parameterTypes = new ArrayList<String>();

    if (paramExpr != null && paramExpr.expressionParam() != null) {
      // Process each parameter expression
      for (var exprParam : paramExpr.expressionParam()) {
        final var exprCtx = exprParam.expression();
        final var argTemp = context.generateTempName();
        final var argDetails = new VariableDetails(argTemp,
            new org.ek9lang.compiler.phase7.support.BasicDetails(scopeId, null));

        // Generate instructions to evaluate the argument expression
        final var exprDetails = new org.ek9lang.compiler.phase7.support.ExprProcessingDetails(exprCtx, argDetails);
        final var argEvaluation = exprGenerator.apply(exprDetails);
        instructions.addAll(variableMemoryManagement.apply(() -> argEvaluation, argDetails));

        // Collect argument variable and type
        argumentVariables.add(argTemp);
        final var argSymbol = getRecordedSymbolOrException(exprCtx);
        final var argType = typeNameOrException.apply(argSymbol);
        parameterTypes.add(argType);
      }
    }

    return new ArgumentDetails(argumentVariables, parameterTypes);
  }

  /**
   * Resolve the return type of a function call.
   */
  private String resolveReturnType(final ISymbol functionSymbol) {
    return typeNameOrException.apply(functionSymbol);
  }

  /**
   * Extract metadata from function symbol.
   */
  private CallMetaData extractMetaData(final ISymbol functionSymbol) {

    final var metaDataExtractor = new CallMetaDataExtractor(context.getParsedModule().getEk9Types());
    return functionSymbol != null ? metaDataExtractor.apply(functionSymbol) : CallMetaData.defaultMetaData();
  }

  /**
   * Record to hold argument processing results.
   */
  private record ArgumentDetails(List<String> argumentVariables, List<String> parameterTypes) {
  }
}