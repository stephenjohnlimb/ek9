package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a public Java field/property that can be exposed as an EK9 record property.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)

public @interface Ek9Property {
  String value();
}
