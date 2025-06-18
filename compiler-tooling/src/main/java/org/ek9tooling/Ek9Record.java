package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 record.
 * But it may be necessary to describe the inheritance structure rather than just use the default 'clasName'.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9Record {
  String value() default "className";
}
