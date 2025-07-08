package org.ek9introspection;

import java.util.function.Predicate;

/**
 * Determines if a class has been defined in generic terms or not.
 */
class DefinedAsEk9Generic implements Predicate<Class<?>> {

  private final ValueFromConstruct valueFromConstruct = new ValueFromConstruct();

  @Override
  public boolean test(final Class<?> cls) {
    final var declaration = valueFromConstruct.apply(cls);
    return declaration.map(s -> s.contains(" of type ")).orElse(false);
  }
}
