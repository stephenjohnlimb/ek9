package org.ek9lang.compiler.phase7.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ExprProcessingDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.symbols.Symbol;
import org.ek9lang.core.CompilerException;

/**
 * Unified processor for constructor call generation.
 * Eliminates duplication between ExprInstrGenerator and CallInstrGenerator
 * by providing a single, reusable component for constructor call processing.
 */
public final class ConstructorCallProcessor {

  private final IRGenerationContext stackContext;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();

  public ConstructorCallProcessor(final IRGenerationContext stackContext) {
    this.stackContext = stackContext;
  }

  /**
   * Process a constructor call and generate appropriate IR instructions.
   * This method consolidates all the common logic between expression and statement contexts.
   *
   * @param callSymbol          The resolved call symbol
   * @param callContext         The ANTLR call context
   * @param resultVariable      The variable to store the constructor result
   * @param instructions        The list to add generated instructions to
   * @param expressionProcessor Function to process argument expressions
   * @param useMemoryManagement Whether to apply memory management to arguments
   */
  public void processConstructorCall(final CallSymbol callSymbol,
                                     final EK9Parser.CallContext callContext,
                                     final String resultVariable,
                                     final List<IRInstr> instructions,
                                     final Function<ExprProcessingDetails, List<IRInstr>> expressionProcessor,
                                     final boolean useMemoryManagement) {

    final var resolvedSymbol = callSymbol.getResolvedSymbolToCall();

    if (!(resolvedSymbol instanceof MethodSymbol methodSymbol) || !methodSymbol.isConstructor()) {
      throw new CompilerException(
          "Expected constructor MethodSymbol, but got: " + resolvedSymbol.getClass().getSimpleName());
    }

    // 1. Type Name Extraction (identical in both generators)
    final var parentScope = methodSymbol.getParentScope();
    final var typeName = (parentScope instanceof Symbol symbol)
        ? symbol.getFullyQualifiedName()
        : parentScope.toString();

    // 2. Debug Info Creation (identical in both generators)
    final var debugInfo = stackContext.createDebugInfo(callSymbol.getSourceToken());

    // 3. Parameter Types Extraction (identical in both generators)
    final var parameterTypes = methodSymbol.getCallParameters().stream()
        .map(typeNameOrException)
        .toList();

    // 4. Argument Processing (unified logic)
    final var argumentVariables = processCallArguments(
        callContext, instructions, expressionProcessor, useMemoryManagement);

    // 5. Metadata Extraction (identical in both generators)
    final var metaData = extractCallMetaData(methodSymbol);

    // 6. CallDetails Creation & Instruction Generation (identical in both generators)
    final var callDetails = new CallDetails(
        typeName,          // targetType
        typeName,          // targetTypeName
        IRConstants.INIT_METHOD, // methodName
        parameterTypes,    // parameterTypes
        typeName,          // returnType
        argumentVariables, // argumentVariables
        metaData          // metaData
    );

    instructions.add(CallInstr.constructor(resultVariable, debugInfo, callDetails));
  }

  /**
   * Process call arguments - unified logic for both contexts.
   * This replaces the separate processCallArguments/processParameters methods.
   */
  private List<String> processCallArguments(final EK9Parser.CallContext callContext,
                                            final List<IRInstr> instructions,
                                            final Function<ExprProcessingDetails, List<IRInstr>> expressionProcessor,
                                            final boolean useMemoryManagement) {

    final var argumentVariables = new ArrayList<String>();

    if (callContext.paramExpression() != null && callContext.paramExpression().expressionParam() != null) {
      // Process each parameter expression
      for (var exprParam : callContext.paramExpression().expressionParam()) {
        final var exprCtx = exprParam.expression();
        final var argTemp = stackContext.generateTempName();
        // STACK-BASED: Get scope ID from current stack frame instead of parameter
        final var argDetails = new VariableDetails(argTemp, new BasicDetails(stackContext.currentScopeId(), null));

        // Generate instructions to evaluate the argument expression
        final var exprDetails = new ExprProcessingDetails(exprCtx, argDetails);
        final var argEvaluation = expressionProcessor.apply(exprDetails);

        if (useMemoryManagement) {
          // Apply memory management (CallInstrGenerator pattern)
          final var variableMemoryManagement = new VariableMemoryManagement();
          instructions.addAll(variableMemoryManagement.apply(() -> argEvaluation, argDetails));
        } else {
          // Direct instruction addition (ExprInstrGenerator pattern)
          instructions.addAll(argEvaluation);
        }

        // Collect argument variable
        argumentVariables.add(argTemp);
      }
    }

    return argumentVariables;
  }

  /**
   * Extract call metadata for the given symbol.
   */
  private CallMetaDataDetails extractCallMetaData(final ISymbol symbol) {
    final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    return symbol != null ? metaDataExtractor.apply(symbol) : CallMetaDataDetails.defaultMetaData();
  }
}