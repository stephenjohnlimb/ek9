package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;
import org.ek9lang.compiler.phase7.calls.CallContext;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.ArgumentDetails;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Generates IR instructions for function/method calls.
 * <p>
 * Handles:<br>
 * - Function calls: functionName(args...)<br>
 * - Method calls: object.methodName(args...)<br>
 * - Parameter evaluation with proper memory management<br>
 * - Return type resolution and metadata extraction<br>
 * - CallInstr.call generation for function invocations<br>
 * </p>
 */
public final class CallInstrGenerator extends AbstractGenerator
    implements BiFunction<EK9Parser.CallContext, VariableDetails, List<IRInstr>> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();
  private final GeneratorSet generators;

  /**
   * Constructor accepting GeneratorSet for unified access to all generators.
   */
  CallInstrGenerator(final IRGenerationContext stackContext, final GeneratorSet generators) {
    super(stackContext);
    this.generators = generators;
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
      // Method call on primary reference: this.method(args...) or super.method(args...)
      return generatePrimaryReferenceCall(ctx, resultDetails);
    } else if (ctx.parameterisedType() != null) {
      // Constructor call: Type()
      return generateConstructorCall(ctx, resultDetails);
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
      final var argumentDetails = processParameters(paramExpr, instructions);

      // Generate different IR based on what we're calling
      if (resolvedSymbol instanceof FunctionSymbol functionSymbol) {
        // Pattern 1: Named function call - need FUNCTION_INSTANCE
        return generateNamedFunctionCall(instructions, functionSymbol, argumentDetails,
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
                                                  final VariableDetails resultDetails,
                                                  final EK9Parser.CallContext ctx) {

    final var fullyQualifiedFunctionName = functionSymbol.getFullyQualifiedName();
    final var debugInfo = debugInfoCreator.apply(new Ek9Token(ctx.identifierReference().start));

    // Step 1: Get the singleton instance of the function
    final var functionInstanceVar = stackContext.generateTempName();
    instructions.add(MemoryInstr.functionInstance(functionInstanceVar, fullyQualifiedFunctionName, debugInfo));

    // Step 2: Create CallContext for cost-based resolution and promotion
    final var callContext = CallContext.forFunctionCall(
        functionSymbol, // Target type (the function symbol)
        argumentDetails.argumentSymbols(), // Argument types as ISymbol
        "_call", // Method: always "_call" for functions
        functionInstanceVar, // Target variable
        argumentDetails.argumentVariables(), // Argument variables
        stackContext.currentScopeId(),          // STACK-BASED: Get scope ID from current stack frame
        ctx // Parse context for symbol resolution
    );

    // Step 3: Use CallDetailsBuilder to get call details with any promotions
    final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);

    // Step 4: Add any promotion instructions
    instructions.addAll(callDetailsResult.allInstructions());

    // Step 5: Fix the target type name to be the function name, not the return type
    final var correctedCallDetails = new CallDetails(
        callDetailsResult.callDetails().targetObject(),
        fullyQualifiedFunctionName, // Use function name as target type, not return type
        callDetailsResult.callDetails().methodName(),
        callDetailsResult.callDetails().parameterTypes(),
        callDetailsResult.callDetails().returnTypeName(),
        callDetailsResult.callDetails().arguments(), // Use promoted arguments
        callDetailsResult.callDetails().metaData(),
        false
    );

    // Step 6: Add the actual call instruction with corrected target type
    // Use whatever target variable the calling mechanism decided on
    instructions.add(CallInstr.call(resultDetails.resultVariable(), debugInfo, correctedCallDetails));
    return instructions;
  }

  /**
   * Generate IR for constructor calls: Type() or Type(args...)
   * Uses unified constructor call processor with memory management.
   */
  private List<IRInstr> generateConstructorCall(final EK9Parser.CallContext ctx,
                                                final VariableDetails resultDetails) {

    final var instructions = new ArrayList<IRInstr>();

    // Get call symbol - all calls go through CallSymbol in Phase 3 resolution
    final var symbol = getRecordedSymbolOrException(ctx);
    if (symbol instanceof CallSymbol callSymbol) {
      final var resolvedSymbol = callSymbol.getResolvedSymbolToCall();

      if (resolvedSymbol instanceof MethodSymbol methodSymbol && methodSymbol.isConstructor()) {
        // Constructor calls always return a value, so create temp variable if needed
        String constructorResultVar = resultDetails.resultVariable();
        if (constructorResultVar == null) {
          // Statement context: create temp variable for constructor result
          constructorResultVar = stackContext.generateTempName();
          // Note: No memory management needed - constructor calls handle object lifecycle
        }

        // Use unified constructor call processor (with memory management for statement context)
        generators.constructorCallProcessor.processConstructorCall(
            callSymbol,
            ctx,
            constructorResultVar,
            instructions,
            generators.exprGenerator,  // Expression processor function
            true                   // Use memory management for statement context
        );
      } else {
        throw new CompilerException("Expected constructor symbol, but got: "
            + resolvedSymbol.getClass().getSimpleName() + " - " + resolvedSymbol);
      }
    } else {
      throw new CompilerException("Expected CallSymbol for constructor, but got: " + symbol.getClass().getSimpleName());
    }

    return instructions;
  }

  /**
   * Generate IR for method calls on primary references: this.method(args...) or super.method(args...)
   */
  private List<IRInstr> generatePrimaryReferenceCall(final EK9Parser.CallContext ctx,
                                                     final VariableDetails resultDetails) {
    final var instructions = new ArrayList<IRInstr>();

    // Get call symbol - all calls go through CallSymbol in Phase 3 resolution
    final var symbol = getRecordedSymbolOrException(ctx);
    if (symbol instanceof CallSymbol callSymbol) {
      final var resolvedSymbol = callSymbol.getResolvedSymbolToCall();

      if (resolvedSymbol instanceof MethodSymbol methodSymbol) {
        if (methodSymbol.isConstructor()) {
          // Constructor calls always return a value, so create temp variable if needed
          String constructorResultVar = resultDetails.resultVariable();
          if (constructorResultVar == null) {
            // Statement context: create temp variable for constructor result
            constructorResultVar = stackContext.generateTempName();
            // Note: No memory management needed - constructor calls handle object lifecycle
          }

          // Constructor calls like super() use the constructor call processor
          generators.constructorCallProcessor.processConstructorCall(
              callSymbol,
              ctx,
              constructorResultVar,
              instructions,
              generators.exprGenerator,
              true  // Use memory management for statement context
          );
        } else {
          // Regular method calls like this.someMethod()

          // Process parameters first (similar to function calls)
          final var paramExpr = ctx.paramExpression();
          final var argumentDetails = processParameters(paramExpr, instructions);

          // Determine the target variable based on primary reference type
          final String targetVariable;
          if (ctx.primaryReference().THIS() != null) {
            targetVariable = "this";
          } else if (ctx.primaryReference().SUPER() != null) {
            targetVariable = "super";
          } else {
            throw new CompilerException("Unknown primary reference type: " + ctx.primaryReference().getText());
          }

          // Get the target type from the method symbol's parent
          final var targetTypeScope = methodSymbol.getParentScope();
          if (!(targetTypeScope instanceof ISymbol targetType)) {
            throw new CompilerException(
                "Method parent scope must be a symbol (AggregateSymbol), but got: " + targetTypeScope.getClass()
                    .getSimpleName());
          }

          // Get the return type from the method symbol
          final var returnType = methodSymbol.getReturningSymbol().getType().orElse(null);

          // Create call context with all required parameters
          final var callContext = new CallContext(
              targetType,
              targetVariable,
              methodSymbol.getName(),
              argumentDetails.argumentSymbols(),
              argumentDetails.argumentVariables(),
              stackContext.currentScopeId(),          // STACK-BASED: Get scope ID from current stack frame
              returnType,                              // Return type from method signature
              ctx
          );

          final var callDetailsResult = generators.callDetailsBuilder.apply(callContext);
          final var debugInfo = instructionBuilder.createDebugInfo(new Ek9Token(ctx.primaryReference().start));

          // Add any promotion instructions that were generated
          instructions.addAll(callDetailsResult.allInstructions());

          // Create the method call instruction
          instructions.add(CallInstr.call(resultDetails.resultVariable(), debugInfo, callDetailsResult.callDetails()));
        }
      } else {
        throw new CompilerException(
            "Primary reference calls must resolve to methods, but got: " + resolvedSymbol.getClass().getSimpleName());
      }
    } else {
      throw new CompilerException(
          "Expected CallSymbol for primary reference call, but got: " + symbol.getClass().getSimpleName());
    }

    return instructions;
  }

  /**
   * Process parameter expressions and return argument details.
   * This is still needed for function calls (not constructor calls).
   */
  private ArgumentDetails processParameters(final EK9Parser.ParamExpressionContext paramExpr,
                                            final List<IRInstr> instructions) {

    final var argumentVariables = new ArrayList<String>();
    final var parameterTypes = new ArrayList<String>();
    final var argumentSymbols = new ArrayList<ISymbol>();

    if (paramExpr != null && paramExpr.expressionParam() != null) {
      // Process each parameter expression
      for (var exprParam : paramExpr.expressionParam()) {
        final var exprCtx = exprParam.expression();
        final var argTemp = stackContext.generateTempName();
        // STACK-BASED: Get scope ID from current stack frame instead of parameter
        final var argDetails = new VariableDetails(argTemp, null);

        // Generate instructions to evaluate the argument expression
        final var exprDetails = new ExprProcessingDetails(exprCtx, argDetails);
        final var argEvaluation = generators.exprGenerator.apply(exprDetails);
        instructions.addAll(generators.variableMemoryManagement.apply(() -> argEvaluation, argDetails));

        // Collect argument variable, type, and symbol
        argumentVariables.add(argTemp);
        final var argSymbol = getRecordedSymbolOrException(exprCtx);

        final var resolvedTypeSymbol = symbolTypeOrException.apply(argSymbol);
        argumentSymbols.add(resolvedTypeSymbol);
        final var argType = typeNameOrException.apply(resolvedTypeSymbol);
        parameterTypes.add(argType);
      }
    }

    return new ArgumentDetails(argumentVariables, parameterTypes, argumentSymbols);
  }
}