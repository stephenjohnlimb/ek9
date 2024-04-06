package org.ek9lang.compiler.phase3;

import static org.ek9lang.compiler.support.SymbolFactory.DEFAULTED;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Accesses the aggregate and gets just the 'default' operators defined on that aggregate (not any hierarchy).
 */
final class RetrieveDefaultedOperators implements Function<IAggregateSymbol, List<MethodSymbol>> {
  @Override
  public List<MethodSymbol> apply(final IAggregateSymbol aggregate) {

    return aggregate.getAllNonAbstractMethodsInThisScopeOnly()
        .stream()
        .filter(MethodSymbol::isOperator)
        .filter(method -> "TRUE".equals(method.getSquirrelledData(DEFAULTED)))
        .toList();

  }
}
