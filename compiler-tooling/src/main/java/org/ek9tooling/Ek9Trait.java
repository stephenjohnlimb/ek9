package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 trait.
 * But note that traits can themselves have traits. For example:
 * <pre>
 * Processor with trait of Moniterable, CostAssessment
 * </pre>
 * <p>
 * So when defining it will be necessary to declare the traits used, rather than just defaulting to the className.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9Trait {
  String value() default "className";
}
