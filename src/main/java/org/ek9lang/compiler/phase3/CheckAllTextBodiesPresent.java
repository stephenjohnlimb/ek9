package org.ek9lang.compiler.phase3;

import java.util.function.Consumer;
import java.util.function.Predicate;
import org.ek9lang.compiler.common.ErrorListener;
import org.ek9lang.compiler.common.SymbolAndScopeManagement;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.AggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Checks that all the text bodies that are on the super are also present in the language variant.
 */
final class CheckAllTextBodiesPresent extends TypedSymbolAccess implements Consumer<AggregateSymbol> {

  /**
   * Check various aspects of overriding methods.
   */
  CheckAllTextBodiesPresent(final SymbolAndScopeManagement symbolAndScopeManagement,
                            final ErrorListener errorListener) {
    super(symbolAndScopeManagement, errorListener);
  }

  @Override
  public void accept(final AggregateSymbol aggregateSymbol) {

    aggregateSymbol.getSuperAggregate().ifPresent(supperTextAggregate -> {

      Predicate<MethodSymbol> isMethodMissing = methodToCheck ->
          aggregateSymbol.resolveMatchingMethodsInThisScopeOnly(new MethodSymbolSearch(methodToCheck),
              new MethodSymbolSearchResult()).isEmpty();

      //We can only report on a single error on the aggregate, so so at the first one that is missing.
      var firstMissingMethod = supperTextAggregate.getAllNonAbstractMethodsInThisScopeOnly()
          .stream()
          .filter(MethodSymbol::isNotConstructor)
          .filter(isMethodMissing)
          .findFirst();

      firstMissingMethod.ifPresent(missingMethod -> emitMissingTextMethodError(aggregateSymbol, missingMethod));
    });
  }

  private void emitMissingTextMethodError(final AggregateSymbol aggregateSymbol,
                                          final MethodSymbol missingMethod) {
    var msg = "'" + missingMethod.getFriendlyName() + "':";
    errorListener.semanticError(aggregateSymbol.getSourceToken(), msg,
        ErrorListener.SemanticClassification.TEXT_METHOD_MISSING);
  }
}
