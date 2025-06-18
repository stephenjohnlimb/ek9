package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 component.
 * But note that components can be abstract and can also extend abstract components, For example:
 * <pre>
 * HRSystem as abstract
 * ...
 * SimpleHRSystem is HRSystem
 * </pre>
 * <p>
 * So when defining it will be necessary to declare the components in EK9 form (irrespective of how implemented).
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9Component {
  String value() default "className";
}
