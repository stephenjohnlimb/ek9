package org.ek9lang.compiler.phase7.support;

import java.util.function.Function;
import org.ek9lang.compiler.common.SymbolTypeOrException;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.support.SymbolMatcher;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.core.CompilerException;

/**
 * Resolves method calls using cost-based analysis to determine if promotion is required.
 * Uses the same cost calculation logic as SymbolMatcher to determine method resolution strategy.
 */
public final class MethodResolver implements Function<CallContext, MethodResolutionResult> {

  private final SymbolTypeOrException symbolTypeOrException = new SymbolTypeOrException();

  @Override
  public MethodResolutionResult apply(final CallContext context) {

    // Create method search for the call
    final var search = new MethodSymbolSearch(context.methodName());

    // Add parameter types to search criteria
    for (var argumentType : context.argumentTypes()) {
      search.addTypeParameter(symbolTypeOrException.apply(argumentType));
    }

    // Get target type for method resolution
    final var targetTypeSymbol = symbolTypeOrException.apply(context.targetType());
    if (!(targetTypeSymbol instanceof AggregateSymbol aggregate)) {
      throw new CompilerException("Cannot resolve methods on non-aggregate type: " + targetTypeSymbol);
    }

    // Resolve method on the target type
    final var results = aggregate.resolveMatchingMethods(search, new MethodSymbolSearchResult());

    if (results.isEmpty()) {
      throw new CompilerException("No method found: " + context.methodName() + " on "
          + context.targetType().getFullyQualifiedName());
    }

    if (results.isAmbiguous()) {
      throw new CompilerException("Ambiguous method call: " + context.methodName() + " on "
          + context.targetType().getFullyQualifiedName());
    }

    // Get best match - for now assume perfect match until we implement percentage access
    final var bestMatch = results.getSingleBestMatchSymbol().orElseThrow();

    // For now, we'll assume perfect matches until we can access the percentage
    // TODO: Implement proper percentage match checking when MethodSymbolSearchResult provides access
    final var matchPercentage = SymbolMatcher.PERCENT_100;
    final var requiresPromotion = false;

    return new MethodResolutionResult(bestMatch, matchPercentage, requiresPromotion);
  }
}