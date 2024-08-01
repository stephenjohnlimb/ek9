package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolsAndScopes;
import org.ek9lang.compiler.common.TypedSymbolAccess;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks that all the text bodies that are on the super are also present in the language variant.
 */
final class AllTextBodiesPresentOrError extends TypedSymbolAccess implements Consumer<AggregateSymbol> {

  /**
   * Check text body for text constructs is present across all language variants.
   */
  AllTextBodiesPresentOrError(final SymbolsAndScopes symbolsAndScopes,
                              final ErrorListener errorListener) {

    super(symbolsAndScopes, errorListener);

  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    aggregateSymbol.getSuperAggregate().ifPresent(supperTextAggregate -> {

      Predicate<MethodSymbol> isMethodMissing = methodToCheck ->
          aggregateSymbol.resolveMatchingMethodsInThisScopeOnly(new MethodSymbolSearch(methodToCheck),
              new MethodSymbolSearchResult()).isEmpty();

      //We can only report on a single error on the aggregate, so so at the first one that is missing.
      final var firstMissingMethod = supperTextAggregate.getAllNonAbstractMethodsInThisScopeOnly()
          .stream()
          .filter(MethodSymbol::isNotConstructor)
          .filter(isMethodMissing)
          .findFirst();

      firstMissingMethod.ifPresent(missingMethod -> emitMissingTextMethodError(aggregateSymbol, missingMethod));
    });

  }

  private void emitMissingTextMethodError(final AggregateSymbol aggregateSymbol,
                                          final MethodSymbol missingMethod) {

    final var msg = "'" + missingMethod.getFriendlyName() + "':";
    errorListener.semanticError(aggregateSymbol.getSourceToken(), msg,
        ErrorListener.SemanticClassification.TEXT_METHOD_MISSING);

  }
}
