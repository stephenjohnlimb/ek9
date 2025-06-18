package org.ek9tooling.introspection;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.function.Function;
import org.ek9tooling.Ek9Property;

/**
 * Just takes a EK9 annotated field, checks for the appropriate annotation and extracts the
 * value from that annotation. If it is suitable then it is used. Otherwise, just the field name
 * is returned.
 */
class ValueFromProperty implements Function<Field, Optional<String>> {
  @Override
  public Optional<String> apply(final Field field) {

    //A bit messy because we cannot have a common super Annotation with abstract value() method on it.
    Annotation[] annotations = field.getAnnotations();
    for (var annotation : annotations) {

      if (annotation instanceof Ek9Property as) {
        return appropriateValueFor(field, as.value());
      }
    }

    return Optional.empty();
  }

  private Optional<String> appropriateValueFor(final Field field, final String proposedValue) {

    //Use the name of the field if the annotation value has not been set.
    if ("fieldName".equals(proposedValue)) {
      return Optional.of(field.getName());
    }
    return Optional.of(proposedValue);

  }
}
