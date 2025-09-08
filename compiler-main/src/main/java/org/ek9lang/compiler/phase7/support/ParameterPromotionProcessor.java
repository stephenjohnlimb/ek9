package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.common.TypeNameOrException;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallInstr;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.compiler.support.TypeCoercions;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.Ek9Token;
import org.ek9lang.core.CompilerException;

/**
 * Processes method call parameters to determine if promotion is required.
 * Implements parameter-by-parameter checking following EK9's cost-based method resolution.
 * Only allows single promotion per parameter as per EK9 design rules.
 */
public final class ParameterPromotionProcessor
    implements BiFunction<CallContext, MethodResolutionResult, PromotionResult> {

  private final TypeNameOrException typeNameOrException = new TypeNameOrException();
  private final IRContext irContext;

  public ParameterPromotionProcessor(final IRContext irContext) {
    this.irContext = irContext;
  }

  @Override
  public PromotionResult apply(final CallContext context, final MethodResolutionResult methodResolution) {

    // If perfect match, no promotion needed
    if (methodResolution.isPerfectMatch()) {
      return new PromotionResult(context.argumentVariables(), List.of());
    }

    final var promotedArguments = new ArrayList<String>();
    final var promotionInstructions = new ArrayList<IRInstr>();

    final var methodParameters = methodResolution.methodSymbol().getSymbolsForThisScope();

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
          final var promotedVar = generatePromoteCall(argumentType, argumentVariable, i);
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
                                               final int paramIndex) {

    final var promotedVar = irContext.generateTempName();
    final var argumentTypeName = typeNameOrException.apply(argumentType);
    final var promotedTypeName = typeNameOrException.apply(getPromotedType(argumentType));

    // Create CallDetails for promotion: argumentVariable._promote() -> promotedVar
    final var promoteCallDetails = new CallDetails(
        argumentVariable,
        argumentTypeName,
        "_promote",  // #^ operator maps to _promote method
        List.of(),   // No parameters for promote
        promotedTypeName,
        List.of(),   // No arguments for promote
        CallMetaData.defaultMetaData()
    );

    // Generate the promotion call instruction
    final var debugInfoCreator = new DebugInfoCreator(irContext);
    final var debugInfo = debugInfoCreator.apply(new Ek9Token("promote_param_" + paramIndex, 0, "IR_GEN"));
    final var promoteInstr = CallInstr.operator(promotedVar, debugInfo, promoteCallDetails);

    return PromotedVariable.of(promotedVar, List.of(promoteInstr));
  }

  /**
   * Get the type that results from promoting the given type using #^.
   * This looks up the return type of the #^ method on the argument type.
   */
  private ISymbol getPromotedType(final ISymbol argumentType) {
    // For now, we'll need to implement actual promotion type lookup
    // This is a simplified version - real implementation should look up #^ method return type
    throw new CompilerException("Promotion type lookup not yet fully implemented for: "
        + argumentType.getFullyQualifiedName());
  }
}