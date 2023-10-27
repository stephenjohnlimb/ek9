package org.ek9lang.compiler.common;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Test to see if an aggregate mandated pure in its construction.
 */
public class AggregateHasPureConstruction implements Predicate<IAggregateSymbol> {
  @Override
  public boolean test(IAggregateSymbol aggregateSymbol) {
    return aggregateSymbol.getAllNonAbstractMethodsInThisScopeOnly()
        .stream()
        .filter(MethodSymbol::isConstructor)
        .anyMatch(MethodSymbol::isMarkedPure);
  }
}
