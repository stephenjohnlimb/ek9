package org.ek9introspection;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This is the main introspector for simple Ek9 constructs.
 */
class GeneralConstructIntrospector extends Introspector implements Consumer<Map<Class<?>, Map<String, Class<?>>>> {

  private final Class<? extends Annotation> annotationType;

  GeneralConstructIntrospector(final PrintStream printStream,
                               final Class<? extends Annotation> annotationType) {
    super(printStream);
    this.annotationType = annotationType;
  }

  @Override
  public void accept(final Map<Class<?>, Map<String, Class<?>>> constructs) {

    final var classes = constructs.get(annotationType);
    if (classes == null || classes.isEmpty()) {
      return;
    }

    outputDefines(annotationType);

    classes
        .keySet()
        .stream()
        .sorted()
        .forEach(key -> introspectClass(classes.get(key)));
  }
}
