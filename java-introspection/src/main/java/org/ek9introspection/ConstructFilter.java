package org.ek9introspection;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;

/**
 * Design just to enable the filtering of constructs by annotation type and a predicate.
 */
final class ConstructFilter
    implements BiFunction<Map<Class<?>, Map<String, Class<?>>>, Predicate<Class<?>>, List<? extends Class<?>>> {

  private final Class<? extends Annotation> annotationType;

  ConstructFilter(final Class<? extends Annotation> annotationType) {
    this.annotationType = annotationType;
  }


  @Override
  public List<? extends Class<?>> apply(final Map<Class<?>, Map<String, Class<?>>> constructs,
                                        final Predicate<Class<?>> predicate) {

    final var all = constructs.get(annotationType);

    return all
        .keySet()
        .stream()
        .sorted()
        .map(all::get)
        .filter(predicate)
        .toList();

  }
}
