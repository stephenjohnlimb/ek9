package org.ek9lang.compiler.support;

import java.util.function.Predicate;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Given a Symbol that is some form of type, check that it could receive some piped data.
 */
public class TypeCanReceivePipedData implements Predicate<ISymbol> {
  @Override
  public boolean test(final ISymbol symbolType) {
    if (symbolType instanceof IAggregateSymbol aggregate) {
      return aggregateHasPipeOperator(aggregate);
    }
    return false;
  }

  /**
   * Just checking that the aggregate has pipe operator.
   */
  private boolean aggregateHasPipeOperator(final IAggregateSymbol aggregate) {
    return aggregate.getAllOperators().stream()
        .anyMatch(operator -> "|".equals(operator.getName()));
  }
}
