package org.ek9lang.compiler.phase3;

import java.util.function.BiPredicate;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Given an aggregate and a symbol, this predicate checks if the symbol is a property/field of that aggregate.
 */
final class IsPropertyOfAggregate implements BiPredicate<IAggregateSymbol, ISymbol> {
  @Override
  public boolean test(final IAggregateSymbol aggregate, final ISymbol possibleProperty) {

    return aggregate
        .getProperties()
        .stream()
        .anyMatch(property -> property.getSourceToken().equals(possibleProperty.getSourceToken()));
  }
}
