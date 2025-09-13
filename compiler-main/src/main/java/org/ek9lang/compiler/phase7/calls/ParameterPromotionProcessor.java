package org.ek9lang.compiler.phase7.calls;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.CallInstr;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.phase7.generation.DebugInfoCreator;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.MethodResolutionResult;
import org.ek9lang.compiler.phase7.support.PromotedVariable;
import org.ek9lang.compiler.phase7.support.PromotionResult;
import org.ek9lang.compiler.phase7.support.VariableDetails;
import org.ek9lang.compiler.phase7.support.VariableMemoryManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.compiler.support.TypeCoercions;
import org.ek9lang.compiler.symbols.IScopedSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.core.CompilerException;

/**
 * Processes method call parameters to determine if promotion is required.
 * Implements parameter-by-parameter checking following EK9's cost-based method resolution.
 * Only allows single promotion per parameter as per EK9 design rules.
 */
public final class ParameterPromotionProcessor
    implements BiFunction<CallContext, MethodResolutionResult, PromotionResult> {

  private static final String PROMOTE_OPERATOR = "#^";
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final OperatorMap operatorMap = new OperatorMap();
  private final IRGenerationContext stackContext;
  private final VariableMemoryManagement variableMemoryManagement;

  public ParameterPromotionProcessor(final IRGenerationContext stackContext) {
    this.stackContext = stackContext;
    this.variableMemoryManagement = new VariableMemoryManagement(stackContext);
  }

  @Override
  public PromotionResult apply(final CallContext context, final MethodResolutionResult methodResolution) {

    // If perfect match, no promotion needed
    if (methodResolution.isPerfectMatch()) {
      return new PromotionResult(context.argumentVariables(), List.of());
    }

    return processParameterPromotion(context, methodResolution.methodSymbol());
  }

  /**
   * Apply parameter promotion for any scoped symbol (method or function).
   * This allows reuse of the same promotion logic for both methods and functions.
   */
  public PromotionResult apply(final CallContext context, final IScopedSymbol scopedSymbol) {
    return processParameterPromotion(context, scopedSymbol);
  }

  /**
   * Core parameter promotion logic that works with any IScopedSymbol.
   * This is reusable for both methods and functions.
   */
  private PromotionResult processParameterPromotion(final CallContext context, final IScopedSymbol scopedSymbol) {
    final var promotedArguments = new ArrayList<String>();
    final var promotionInstructions = new ArrayList<IRInstr>();

    final var methodParameters = scopedSymbol.getSymbolsForThisScope();

    // Check each parameter for promotion requirement
    for (int i = 0; i < context.argumentTypes().size(); i++) {
      final var argumentType = context.argumentTypes().get(i);
      final var parameterType = methodParameters.get(i).getType().orElseThrow();
      final var argumentVariable = context.argumentVariables().get(i);

      // Check if direct assignment is possible (no coercion)
      final var directCost = argumentType.getUnCoercedAssignableCostTo(parameterType);

      if (directCost >= SymbolMatcher.ZERO_COST) {
        // Direct assignment - no promotion needed
        promotedArguments.add(argumentVariable);
      } else {
        // Check if promotion is possible
        if (TypeCoercions.isCoercible(argumentType, parameterType)) {
          final var promotedVar = generatePromoteCall(argumentType, argumentVariable, context, i);
          promotedArguments.add(promotedVar.variable());
          promotionInstructions.addAll(promotedVar.instructions());

          // Verify promoted type is compatible (single promotion rule)
          final var promotedType = getPromotedType(argumentType);
          final var finalCost = promotedType.getUnCoercedAssignableCostTo(parameterType);
          if (finalCost < SymbolMatcher.ZERO_COST) {
            throw new CompilerException("Promotion of " + argumentType.getFullyQualifiedName()
                + " does not result in compatible type for parameter " + i
                + ". Required: " + parameterType.getFullyQualifiedName());
          }
        } else {
          throw new CompilerException("Cannot promote argument " + i + " from "
              + argumentType.getFullyQualifiedName() + " to " + parameterType.getFullyQualifiedName());
        }
      }
    }

    return new PromotionResult(promotedArguments, promotionInstructions);
  }

  /**
   * Generate IR instructions to call the #^ promotion operator.
   */
  private PromotedVariable generatePromoteCall(final ISymbol argumentType, final String argumentVariable,
                                               final CallContext callContext,
                                               final int paramIndex) {

    final var promotedVar = stackContext.generateTempName();
    final var argumentTypeName = typeNameOrException.apply(argumentType);
    final var promotedTypeName = typeNameOrException.apply(getPromotedType(argumentType));

    final var operatorDetails = operatorMap.getOperatorDetails(PROMOTE_OPERATOR);
    // Create CallDetails for promotion: argumentVariable._promote() -> promotedVar
    final var promoteCallDetails = new CallDetails(
        argumentVariable,
        argumentTypeName,
        "_promote",  // #^ operator maps to _promote method
        List.of(),   // No parameters for promote
        promotedTypeName,
        List.of(),   // No arguments for promote
        new CallMetaDataDetails(operatorDetails.markedPure(), 0, operatorMap.getSideEffects(PROMOTE_OPERATOR))
    );

    // Generate the promotion call instruction with proper memory management
    final var debugInfoCreator = new DebugInfoCreator(stackContext.getCurrentIRContext());
    // Use call site context for debug info, not method definition
    final var debugInfo = callContext.parseContext() != null
        ? debugInfoCreator.apply(new org.ek9lang.compiler.tokenizer.Ek9Token(callContext.parseContext().start))
        : null; // Should not happen for function calls, but provide fallback
    final var promoteInstr = CallInstr.operator(promotedVar, debugInfo, promoteCallDetails);

    // Use VariableMemoryManagement to properly handle RETAIN and SCOPE_REGISTER with correct scope
    // STACK-BASED: Get scope ID from current stack frame instead of CallContext parameter
    final var promotedVariableDetails = new VariableDetails(promotedVar, debugInfo);

    final var promotionInstructions = variableMemoryManagement.apply(
        () -> new ArrayList<>(List.of(promoteInstr)),
        // Mutable list for the instruction that generates the promoted value
        promotedVariableDetails       // The variable details for memory management
    );

    return PromotedVariable.of(promotedVar, promotionInstructions);
  }

  /**
   * Get the type that results from promoting the given type using #^.
   * This looks up the return type of the #^ method on the argument type.
   */
  private ISymbol getPromotedType(final ISymbol argumentType) {
    // Look up the _promote method (#^ operator) on the argument type
    final var search = new MethodSymbolSearch(PROMOTE_OPERATOR);

    // Get the type symbol for method resolution
    final var typeSymbol = new SymbolTypeOrException().apply(argumentType);

    if (typeSymbol instanceof org.ek9lang.compiler.symbols.AggregateSymbol aggregate) {
      // Resolve the _promote method on the type
      final var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());

      final var promoteMethod = results.getSingleBestMatchSymbol();
      if (promoteMethod.isPresent()) {
        // Return the return type of the _promote method
        return promoteMethod.get().getType().orElseThrow(
            () -> new CompilerException("_promote method has no return type on: "
                + argumentType.getFullyQualifiedName()));
      }
    }

    throw new CompilerException("Cannot find _promote method on type: "
        + argumentType.getFullyQualifiedName());
  }
}