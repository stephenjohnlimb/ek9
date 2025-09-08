package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.CallMetaDataExtractor;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.core.AssertValue;

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

  private final MethodResolver methodResolver;
  private final ParameterPromotionProcessor parameterProcessor;
  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final IRContext irContext;

  public CallDetailsBuilder(final IRContext irContext) {
    AssertValue.checkNotNull("IRContext cannot be null", irContext);
    this.irContext = irContext;
    this.methodResolver = new MethodResolver();
    this.parameterProcessor = new ParameterPromotionProcessor(irContext);
  }

  @Override
  public CallDetailsResult apply(final CallContext context) {
    AssertValue.checkNotNull("CallContext cannot be null", context);

    // Step 1: Resolve method with cost analysis
    final var methodResolution = methodResolver.apply(context);

    if (methodResolution.isInvalid()) {
      throw new RuntimeException("Method resolution failed for: " + context.methodName());
    }

    // Step 2: Process parameters with promotion if needed
    final var promotionResult = parameterProcessor.apply(context, methodResolution);

    // Step 3: Create metadata for the method call
    final var metaData = extractCallMetaData(methodResolution);

    // Step 4: Build CallDetails with processed parameters
    final var callDetails = buildCallDetails(context, methodResolution, promotionResult, metaData);

    // Step 5: Collect all required IR instructions (promotions + call)
    final var allInstructions = new ArrayList<>(promotionResult.promotionInstructions());

    return new CallDetailsResult(callDetails, allInstructions);
  }

  /**
   * Build the final CallDetails object with all resolved information.
   */
  private CallDetails buildCallDetails(final CallContext context, 
                                      final MethodResolutionResult methodResolution,
                                      final PromotionResult promotionResult,
                                      final CallMetaData metaData) {

    // Get target information
    final var targetTypeName = typeNameOrException.apply(context.targetType());
    
    // Get parameter types (from original arguments, not promoted types)
    final var parameterTypes = context.argumentTypes().stream()
        .map(typeNameOrException)
        .toList();

    return new CallDetails(
        getTargetVariable(context),
        targetTypeName,
        context.methodName(),
        parameterTypes,
        methodResolution.getReturnTypeName(),
        promotionResult.promotedArguments(),
        metaData
    );
  }

  /**
   * Get the target variable for the method call.
   * For operators, this is the left operand. For method calls, this is the object instance.
   */
  private String getTargetVariable(final CallContext context) {
    return context.targetVariable();
  }

  /**
   * Extract call metadata from the resolved method.
   */
  private CallMetaData extractCallMetaData(final MethodResolutionResult methodResolution) {
    final var metaDataExtractor = new CallMetaDataExtractor(irContext.getParsedModule().getEk9Types());
    return metaDataExtractor.apply(methodResolution.methodSymbol());
  }

  /**
   * Result of building CallDetails including any promotion instructions needed.
   */
  public record CallDetailsResult(
      CallDetails callDetails,
      List<IRInstr> allInstructions
  ) {
    
    /**
     * Check if any parameter promotions were required.
     */
    public boolean hasPromotions() {
      return !allInstructions.isEmpty();
    }
  }
}