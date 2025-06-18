package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that references a construct from another package.
 * This can be used with any construct so that any and all references can be recorded where they are used.
 * The code that builds the 'extern' module definition from the Java classes will locate this annotation
 * on each of the constructs and extract the references, sort them and ensure they are only included once
 * in the extern definition of the module.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9References {
  String value();
}
