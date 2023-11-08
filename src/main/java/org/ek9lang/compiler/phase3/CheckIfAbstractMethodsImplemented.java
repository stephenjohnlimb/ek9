package org.ek9lang.compiler.phase3;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.search.MethodSymbolSearchResult;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Gets the abstract methods and then resolves each to see what the resulting method would be.
 * It then calls the consumer it has been provided with so it can decide what action if any to take.
 */
final class CheckIfAbstractMethodsImplemented implements BiConsumer<IAggregateSymbol, Consumer<MethodSymbol>> {


  CheckIfAbstractMethodsImplemented() {

  }

  @Override
  public void accept(final IAggregateSymbol aggregateSymbol, final Consumer<MethodSymbol> actionToTake) {
    final List<MethodSymbol> abstractMethodsToCheck = aggregateSymbol.getAllAbstractMethods();

    abstractMethodsToCheck.forEach(methodSymbol -> {
      MethodSymbolSearch search = new MethodSymbolSearch(methodSymbol);
      var result = aggregateSymbol.resolveMatchingMethods(search, new MethodSymbolSearchResult());
      result.getSingleBestMatchSymbol().ifPresent(actionToTake);
    });
  }
}
