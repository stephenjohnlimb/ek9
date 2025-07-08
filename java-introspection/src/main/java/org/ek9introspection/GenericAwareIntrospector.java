package org.ek9introspection;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.ek9tooling.Ek9Class;

/**
 * For dealing with generic and non generic classes and functions.
 */
final class GenericAwareIntrospector extends Introspector implements Consumer<Map<Class<?>, Map<String, Class<?>>>> {

  private final Class<? extends Annotation> annotationType;

  private final DefinedAsEk9Generic definedAsEk9Generic = new DefinedAsEk9Generic();
  private final ConstructFilter constructFilter;
  private final Consumer<Class<?>> introspectionOperation;
  private final boolean outputGenerics;

  GenericAwareIntrospector(final PrintStream printStream,
                           final boolean outputGenerics,
                           final Class<? extends Annotation> annotationType) {
    super(printStream);
    this.outputGenerics = outputGenerics;
    this.annotationType = annotationType;
    this.constructFilter = new ConstructFilter(annotationType);

    if (annotationType.equals(Ek9Class.class)) {
      introspectionOperation = this::introspectClass;
    } else {
      introspectionOperation = this::introspectFunction;
    }
  }

  @Override
  public void accept(final Map<Class<?>, Map<String, Class<?>>> constructs) {

    if (outputGenerics) {
      output(constructs, definedAsEk9Generic);
    } else {
      output(constructs, definedAsEk9Generic.negate());
    }
  }

  private void output(final Map<Class<?>, Map<String, Class<?>>> constructs,
                      final Predicate<Class<?>> predicate) {

    final var filtered = constructFilter.apply(constructs, predicate);
    if (filtered.isEmpty()) {
      return;
    }

    outputDefines(annotationType);
    filtered.forEach(introspectionOperation);
  }

}