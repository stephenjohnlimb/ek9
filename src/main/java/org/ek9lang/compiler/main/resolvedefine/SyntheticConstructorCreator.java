package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.symbol.IAggregateSymbol;
import org.ek9lang.compiler.symbol.support.AggregateFactory;

/**
 * Some aggregates like classes, components, etc. Can be defined without a constructor.
 * If that is the case then a synthetic constructor(s) can be added.
 * This is a bit like Java in terms of not requiring the developer to declare obvious constructors.
 * The ek9 developer can use 'default Name' where Name is the class/record etc.
 */
public class SyntheticConstructorCreator implements Consumer<IAggregateSymbol> {

  private final AggregateFactory aggregateFactory = new AggregateFactory();

  @Override
  public void accept(IAggregateSymbol aggregate) {
    aggregateFactory.addSyntheticConstructorIfRequired(aggregate, List.of());
  }
}
