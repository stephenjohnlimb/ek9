package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.support.AggregateFactory;
import org.ek9lang.compiler.symbols.Ek9Types;
import org.ek9lang.compiler.symbols.IAggregateSymbol;

/**
 * Some aggregates like classes, components, etc. Can be defined without a constructor.
 * If that is the case then a synthetic constructor(s) can be added.
 * This is a bit like Java in terms of not requiring the developer to declare obvious constructors.
 * The ek9 developer can use 'default Name' where Name is the class/record etc.
 */
final class SyntheticConstructorCreator implements Consumer<IAggregateSymbol> {

  private final AggregateFactory aggregateFactory;

  SyntheticConstructorCreator(final Ek9Types ek9Types) {
    this.aggregateFactory = new AggregateFactory(ek9Types);
  }

  @Override
  public void accept(IAggregateSymbol aggregate) {
    aggregateFactory.addSyntheticConstructorIfRequired(aggregate, List.of());
  }
}
