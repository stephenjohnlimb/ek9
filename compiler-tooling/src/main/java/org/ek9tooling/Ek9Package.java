package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 package..
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)

public @interface Ek9Package {
  String value();
}
