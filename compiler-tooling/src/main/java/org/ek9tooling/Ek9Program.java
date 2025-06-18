package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 program.
 * In some cases (when used with applications for example) it will be necessary to
 * add a more complete declaration, rather than just use the className.<br/>
 * For example:
 * <pre>
 * Demonstration() with application of DemoApp
 * </pre>
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9Program {
  String value() default "className";
}
