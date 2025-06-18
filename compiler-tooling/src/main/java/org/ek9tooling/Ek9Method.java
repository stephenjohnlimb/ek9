package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java method that can be exposed as an EK9 method.
 * This nearly always requires specific decoration for pure/abstract etc.
 * In the case of methods on services it requires much more detail:
 * <pre>
 * index() as GET for :/index.html
 *   &lt;- response as HTTPResponse?
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)

public @interface Ek9Method {
  String value();
}
