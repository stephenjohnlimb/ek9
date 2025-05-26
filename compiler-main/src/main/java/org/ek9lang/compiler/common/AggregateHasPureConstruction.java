package org.ek9lang.compiler.common;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Predicate to see if an aggregate mandated pure in its construction.
 */
public class AggregateHasPureConstruction implements Predicate<IAggregateSymbol> {

  @Override
  public boolean test(final IAggregateSymbol aggregateSymbol) {

    return aggregateSymbol.getConstructors().stream().anyMatch(MethodSymbol::isMarkedPure);
  }
}
