package org.ek9lang.compiler.phase2;

import java.util.List;
import java.util.function.Consumer;
import org.ek9lang.compiler.support.AggregateManipulator;
import org.ek9lang.compiler.symbols.IAggregateSymbol;

/**
 * Some aggregates like classes, components, etc. Can be defined without a constructor.
 * <p>
 * If that is the case then a synthetic constructor(s) can be added.
 * This is a bit like Java in terms of not requiring the developer to declare obvious constructors.
 * The EK9 developer can use 'default Name' where Name is the class/record etc.
 * </p>
 */
final class SyntheticConstructorCreator implements Consumer<IAggregateSymbol> {

  private final AggregateManipulator aggregateManipulator;

  SyntheticConstructorCreator(final AggregateManipulator aggregateManipulator) {

    this.aggregateManipulator = aggregateManipulator;

  }

  @Override
  public void accept(final IAggregateSymbol aggregate) {

    aggregateManipulator.addSyntheticConstructorIfRequired(aggregate, List.of());

  }
}
