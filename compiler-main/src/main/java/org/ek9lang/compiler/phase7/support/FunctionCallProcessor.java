package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Unified function call processor using CallDetailsBuilder for method resolution and promotion.
 * Handles both statement and expression contexts with appropriate memory management.
 * <p>
 * This component replaces the duplicate function call logic in CallInstrGenerator and
 * ExprInstrGenerator, providing consistent method resolution and automatic promotion
 * across all function call contexts.
 * </p>
 */
public final class FunctionCallProcessor implements Function<CallProcessingDetails, List<IRInstr>> {

  private final IRContext context;
  private final CallDetailsBuilder callDetailsBuilder;
  private final VariableMemoryManagement variableMemoryManagement;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final DebugInfoCreator debugInfoCreator;
  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();

  public FunctionCallProcessor(final IRContext context) {
    AssertValue.checkNotNull("IRContext cannot be null", context);
    this.context = context;
    this.callDetailsBuilder = new CallDetailsBuilder(context);
    this.variableMemoryManagement = new VariableMemoryManagement();
    this.debugInfoCreator = new DebugInfoCreator(context);
  }

  /**
   * Get recorded symbol or throw exception if not found.
   */
  private ISymbol getRecordedSymbolOrException(org.antlr.v4.runtime.tree.ParseTree node) {
    final var rtn = context.getParsedModule().getRecordedSymbol(node);
    AssertValue.checkNotNull("Symbol should be resolved by phases 1-6", rtn);
    AssertValue.checkTrue("Symbol must have been given a type by phase 7", rtn.getType().isPresent());
    return rtn;
  }

  /**
   * Apply function call processing with an expression processor function.
   * This avoids circular dependencies by accepting the expression processor as a parameter.
   */
  public List<IRInstr> apply(final CallProcessingDetails details,
                             final Function<ExprProcessingDetails, List<IRInstr>> exprProcessor) {
    return processWithExpressionProcessor(details, exprProcessor);
  }

  @Override
  public List<IRInstr> apply(final CallProcessingDetails details) {
    throw new CompilerException(
        "FunctionCallProcessor requires expression processor - use apply(details, exprProcessor) instead");
  }

  /**
   * Process function call with expression processor to avoid circular dependencies.
   */
  private List<IRInstr> processWithExpressionProcessor(final CallProcessingDetails details,
                                                       final Function<ExprProcessingDetails, List<IRInstr>> exprProcessor) {
    AssertValue.checkNotNull("CallProcessingDetails cannot be null", details);
    AssertValue.checkNotNull("Expression processor cannot be null", exprProcessor);

    final var instructions = new ArrayList<IRInstr>();
    final var callContext = details.callContext();

    // Get call symbol - all calls go through CallSymbol in Phase 3 resolution
    final var symbol = getRecordedSymbolOrException(callContext);
    if (!(symbol instanceof CallSymbol callSymbol)) {
      throw new CompilerException("Expected CallSymbol, but got: " + symbol.getClass().getSimpleName());
    }

    final var resolvedSymbol = callSymbol.getResolvedSymbolToCall();

    // Only handle function calls - constructors handled by ConstructorCallProcessor
    if (!(resolvedSymbol instanceof FunctionSymbol functionSymbol)) {
      throw new CompilerException("FunctionCallProcessor only handles FunctionSymbol calls. Got: "
          + resolvedSymbol.getClass().getSimpleName() + " - " + resolvedSymbol);
    }

    // Process function call using unified approach
    return processFunctionCall(callSymbol, functionSymbol, details, instructions, exprProcessor);
  }

  /**
   * Process function call using unified CallDetailsBuilder approach.
   * This replaces the manual CallDetails construction with promotion-aware resolution.
   */
  private List<IRInstr> processFunctionCall(final CallSymbol callSymbol,
                                            final FunctionSymbol functionSymbol,
                                            final CallProcessingDetails details,
                                            final List<IRInstr> instructions,
                                            final Function<ExprProcessingDetails, List<IRInstr>> exprProcessor) {

    final var callContext = details.callContext();

    // Step 1: Process parameters and collect argument details
    final var argumentDetails = processParameters(callContext.paramExpression(), instructions,
        details.scopeId(), exprProcessor);

    // Step 2: Create CallContext for unified method resolution  
    final var methodCallContext = createCallContext(functionSymbol, argumentDetails, details);

    // Step 3: Use CallDetailsBuilder for unified method resolution with promotion
    final var callDetailsResult = callDetailsBuilder.apply(methodCallContext);

    // Step 4: Add any promotion instructions 
    instructions.addAll(callDetailsResult.allInstructions());

    // Step 5: Generate function instance and call instructions
    final var debugInfo = debugInfoCreator.apply(new Ek9Token(callContext.start));

    // Get the singleton instance of the function
    final var functionInstanceVar = context.generateTempName();
    final var fullyQualifiedFunctionName = functionSymbol.getFullyQualifiedName();
    instructions.add(MemoryInstr.functionInstance(functionInstanceVar, fullyQualifiedFunctionName, debugInfo));

    // Step 6: Create call instruction using resolved CallDetails
    final var callDetails = callDetailsResult.callDetails();

    // Override target information for function calls (use function instance)
    final var functionCallDetails = new org.ek9lang.compiler.ir.CallDetails(
        functionInstanceVar,           // Target: the singleton instance variable
        fullyQualifiedFunctionName,    // Target type: the function type
        "_call",                       // Method: always "_call" for functions
        callDetails.parameterTypes(),   // Parameter types from resolution
        callDetails.returnTypeName(),   // Return type from resolution
        callDetails.arguments(), // Arguments (possibly promoted)
        callDetails.metaData()          // Metadata from resolution
    );

    instructions.add(CallInstr.call(details.resultVariable(), debugInfo, functionCallDetails));

    // Step 7: Apply context-appropriate memory management
    return applyMemoryManagement(instructions, details);
  }

  /**
   * Process parameter expressions and return argument details.
   * This handles argument evaluation and creates temporary variables.
   */
  private ArgumentDetails processParameters(final EK9Parser.ParamExpressionContext paramExpr,
                                            final List<IRInstr> instructions,
                                            final String scopeId,
                                            final Function<ExprProcessingDetails, List<IRInstr>> exprProcessor) {

    final var argumentVariables = new ArrayList<String>();
    final var parameterTypes = new ArrayList<String>();
    final var argumentSymbols = new ArrayList<ISymbol>();

    if (paramExpr != null && paramExpr.expressionParam() != null) {
      // Process each parameter expression
      for (var exprParam : paramExpr.expressionParam()) {
        final var exprCtx = exprParam.expression();
        final var argTemp = context.generateTempName();
        final var argDetails = new VariableDetails(argTemp,
            new BasicDetails(scopeId, null));

        // Generate instructions to evaluate the argument expression
        final var exprDetails = new ExprProcessingDetails(exprCtx, argDetails);
        final var argEvaluation = exprProcessor.apply(exprDetails);
        instructions.addAll(variableMemoryManagement.apply(() -> argEvaluation, argDetails));

        // Collect argument variable, type, and symbol
        argumentVariables.add(argTemp);
        final var argSymbol = getRecordedSymbolOrException(exprCtx);

        final var resolvedTypeSymbol = symbolTypeOrException.apply(argSymbol);
        final var argType = typeNameOrException.apply(resolvedTypeSymbol);
        parameterTypes.add(argType);
        argumentSymbols.add(resolvedTypeSymbol);
      }
    }

    return new ArgumentDetails(argumentVariables, parameterTypes, argumentSymbols);
  }

  /**
   * Create CallContext for unified method resolution.
   * This adapts the function call information to the CallContext interface.
   */
  private CallContext createCallContext(final FunctionSymbol functionSymbol,
                                        final ArgumentDetails argumentDetails,
                                        final CallProcessingDetails details) {

    // Function calls use the function symbol as the "target type"
    // The method name is always "_call" for function calls
    return new CallContext(
        functionSymbol,                    // Target type (the function itself)
        context.generateTempName(),        // Target variable (will be function instance)
        "_call",                          // Method name (always "_call" for functions)
        argumentDetails.argumentSymbols(), // Argument types for promotion analysis
        argumentDetails.argumentVariables(), // Argument variables
        details.scopeId(),
        details.callContext()             // Pass the original parse context
    );
  }

  /**
   * Record to hold argument processing results.
   */
  private record ArgumentDetails(
      List<String> argumentVariables,
      List<String> parameterTypes,
      List<ISymbol> argumentSymbols
  ) {
  }

  /**
   * Apply context-appropriate memory management.
   * Statement context gets full memory management, expression context minimal.
   */
  private List<IRInstr> applyMemoryManagement(final List<IRInstr> instructions,
                                              final CallProcessingDetails details) {

    if (details.isStatementContext()) {
      // Statement context: Apply full memory management
      final var memoryManagedInstructions = new ArrayList<IRInstr>();
      final var variableDetails = details.variableDetails();

      // Use VariableMemoryManagement for proper RETAIN/SCOPE_REGISTER patterns
      final var managedInstructions = variableMemoryManagement.apply(() -> instructions, variableDetails);
      memoryManagedInstructions.addAll(managedInstructions);

      return memoryManagedInstructions;
    } else {
      // Expression context: Minimal memory management - instructions as-is
      return instructions;
    }
  }
}