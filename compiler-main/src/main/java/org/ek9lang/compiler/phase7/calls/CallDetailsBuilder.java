package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import static org.ek9lang.compiler.support.EK9TypeNames.EK9_VOID;

import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.support.CallMetaDataExtractor;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.MethodResolutionResult;
import org.ek9lang.compiler.phase7.support.PromotionResult;
import org.ek9lang.compiler.symbols.CallSymbol;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.core.AssertValue;
import org.ek9lang.core.CompilerException;

/**
 * Composable builder for creating CallDetails with cost-based method resolution and promotion support.
 * Integrates method resolution, parameter promotion processing, and metadata extraction
 * into a single reusable component for all CALL operations.
 * <p>
 * This builder handles:<br>
 * - Cost-based method resolution using SymbolMatcher logic<br>
 * - Parameter-by-parameter promotion checking via #^ operator<br>
 * - CallDetails construction with proper type information<br>
 * - Integration with escape analysis metadata<br>
 * </p>
 */
public final class CallDetailsBuilder implements Function<CallContext, CallDetailsBuilder.CallDetailsResult> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final IRGenerationContext stackContext;
  private final ParameterPromotionProcessor parameterProcessor;
  private final OperatorMap operatorMap = new OperatorMap();

  public CallDetailsBuilder(final IRGenerationContext stackContext) {
    AssertValue.checkNotNull("IRGenerationContext cannot be null", stackContext);
    this.stackContext = stackContext;
    this.parameterProcessor = new ParameterPromotionProcessor(stackContext);
  }

  @Override
  public CallDetailsResult apply(final CallContext context) {
    AssertValue.checkNotNull("CallContext cannot be null", context);

    // Use the resolved method from the parse context instead of guessing
    return resolveFromParsedSymbol(context);
  }

  /**
   * Resolve CallDetails using the already resolved method from the parse context.
   * This avoids guessing and uses the actual method symbol with proper metadata.
   */
  private CallDetailsResult resolveFromParsedSymbol(final CallContext context) {
    if (context.parseContext() == null) {
      // Fallback for operators without direct parse context (e.g., assignment operators)
      return resolveUsingSyntheticOperators(context);
    }

    // Get the resolved CallSymbol from the parse context (resolved in Phase 3)
    final var symbol = stackContext.getParsedModule().getRecordedSymbol(context.parseContext());

    if (!(symbol instanceof CallSymbol callSymbol)) {
      throw new CompilerException("Expected CallSymbol, but got: "
          + (symbol != null ? symbol.getClass().getSimpleName() : "null"));
    }

    // Get the actual resolved method symbol
    final var resolvedMethod = callSymbol.getResolvedSymbolToCall();
    if (resolvedMethod == null) {
      throw new CompilerException("CallSymbol has no resolved method: " + context.methodName());
    }

    // Extract metadata from the actual resolved method (no guessing!)
    final var metaDataExtractor = new CallMetaDataExtractor(stackContext.getParsedModule().getEk9Types());
    final var metaData = metaDataExtractor.apply(resolvedMethod);

    // Get proper return type from the resolved method
    final var returnType = typeNameOrException.apply(resolvedMethod);

    // Build CallDetails with actual method information
    final var targetTypeName = typeNameOrException.apply(context.targetType());
    final var parameterTypes = context.argumentTypes().stream()
        .map(typeNameOrException)
        .toList();

    // Check if parameter promotion is needed
    final var promotionResult = checkParameterPromotion(context, resolvedMethod);

    // Check if this is a trait call (requires invokeinterface in JVM bytecode)
    final var isTraitCall = context.targetType().getGenus() == org.ek9lang.compiler.symbols.SymbolGenus.CLASS_TRAIT;

    final var callDetails = new CallDetails(
        context.targetVariable(),
        targetTypeName,
        context.methodName(),
        parameterTypes,
        returnType,
        promotionResult.promotedArguments(),  // Use promoted arguments if any
        metaData,
        isTraitCall
    );

    return new CallDetailsResult(callDetails, promotionResult.promotionInstructions());
  }

  /**
   * Check if parameter promotion is needed for the resolved method.
   * This integrates the existing ParameterPromotionProcessor with our resolved method approach.
   */
  private PromotionResult checkParameterPromotion(final CallContext context, final ISymbol resolvedMethod) {

    // Both methods and functions can have parameter promotion - they both extend ScopedSymbol
    // Create a MethodResolutionResult for the ParameterPromotionProcessor
    return switch (resolvedMethod) {
      case MethodSymbol methodSymbol -> parameterProcessor.apply(context, new MethodResolutionResult(
          methodSymbol, 100.0, false));
      case FunctionSymbol functionSymbol -> parameterProcessor.apply(context, functionSymbol);
      default -> new PromotionResult(context.argumentVariables(), List.of());
    };
  }

  /**
   * OperatorMap-based fallback for operators without parse context.
   * Uses enhanced OperatorMap to get correct metadata instead of hardcoded logic.
   */
  private CallDetailsResult resolveUsingSyntheticOperators(final CallContext context) {
    final var targetTypeName = typeNameOrException.apply(context.targetType());
    final var parameterTypes = context.argumentTypes().stream()
        .map(typeNameOrException)
        .toList();

    // Try to get operator details from OperatorMap by method name (backward lookup)
    String returnType;
    CallMetaDataDetails metaData;

    if (operatorMap.hasMethod(context.methodName())) {
      final var operatorDetails = operatorMap.getOperatorDetailsByMethod(context.methodName());

      // Determine return type based on OperatorDetails
      if (operatorDetails.hasReturn()) {
        returnType = targetTypeName; // Return same type as target for most operators
      } else {
        returnType = EK9_VOID; // No return = Void
      }

      // Get side effects from centralized OperatorMap logic
      final var sideEffects = operatorMap.getSideEffectsByMethod(context.methodName());

      metaData = new CallMetaDataDetails(
          operatorDetails.markedPure(),
          operatorDetails.markedPure() ? 1 : 0, // Simple complexity: 1 for pure, 0 for impure
          Set.copyOf(sideEffects)
      );
    } else {
      throw new CompilerException("Unknown Operator" + context.methodName());
    }

    // Check if this is a trait call (synthetic operators rarely on traits, but check anyway)
    final var isTraitCall = context.targetType().getGenus() == org.ek9lang.compiler.symbols.SymbolGenus.CLASS_TRAIT;

    final var callDetails = new CallDetails(
        context.targetVariable(),
        targetTypeName,
        context.methodName(),
        parameterTypes,
        returnType,
        context.argumentVariables(),
        metaData,
        isTraitCall
    );

    return new CallDetailsResult(callDetails, List.of());
  }


  /**
   * Result of building CallDetails including any promotion instructions needed.
   */
  public record CallDetailsResult(CallDetails callDetails, List<IRInstr> allInstructions) {
  }
}