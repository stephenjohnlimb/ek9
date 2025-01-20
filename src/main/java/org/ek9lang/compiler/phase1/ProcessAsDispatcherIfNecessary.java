package org.ek9lang.compiler.phase1;

import java.util.function.Consumer;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * If any methods are marked as 'dispatcher' then mark this class as a 'dispatcher'.
 */
class ProcessAsDispatcherIfNecessary implements Consumer<ISymbol> {
  @Override
  public void accept(final ISymbol possibleAggregate) {
    if (possibleAggregate instanceof IAggregateSymbol aggregate) {
      aggregate.setMarkedAsDispatcher(
          aggregate.getAllNonAbstractMethods().stream().anyMatch(MethodSymbol::isMarkedAsDispatcher));
    }
  }
}
