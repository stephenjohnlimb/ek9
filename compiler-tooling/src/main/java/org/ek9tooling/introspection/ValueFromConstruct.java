package org.ek9tooling.introspection;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;
import org.ek9tooling.Ek9Application;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Component;
import org.ek9tooling.Ek9ConstrainedType;
import org.ek9tooling.Ek9EnumType;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Package;
import org.ek9tooling.Ek9Program;
import org.ek9tooling.Ek9Record;
import org.ek9tooling.Ek9Service;
import org.ek9tooling.Ek9Text;
import org.ek9tooling.Ek9Trait;

/**
 * Just takes a EK9 annotated class, checks for the appropriate annotation and extracts the
 * value from that annotation. If it is suitable then it is used. Otherwise, just the class SimpleName
 * is returned.
 */
class ValueFromConstruct implements Function<Class<?>, Optional<String>> {
  @Override
  public Optional<String> apply(final Class<?> cls) {

    //A bit messy because we cannot have a common super Annotation with abstract value() method on it.
    Annotation[] annotations = cls.getAnnotations();
    for (var annotation : annotations) {

      if (annotation instanceof Ek9Application as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Class as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Component as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Function as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Package as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Program as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Record as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Service as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Text as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9Trait as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9ConstrainedType as) {
        return appropriateValueFor(cls, as.value());
      }
      if (annotation instanceof Ek9EnumType as) {
        return appropriateValueFor(cls, as.value());
      }
    }
    return Optional.empty();

  }

  private Optional<String> appropriateValueFor(final Class<?> cls, final String proposedValue) {

    final var className = "className";
    if (className.equals(proposedValue)) {
      return Optional.of(cls.getSimpleName());
    }
    return Optional.of(proposedValue);

  }
}
