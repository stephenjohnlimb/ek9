package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 function.
 * Note that it is possible to provide a value here for generic functions as the name of the
 * function will need more EK9 decoration.
 * <p>
 * For example as simple function:
 * </p>
 * <pre>
 * mathOperation() as pure abstract
 *   -&gt; value as Float
 *   &lt;- result as Float?
 *
 * multiply() is mathOperation as pure
 *   -&gt; value as Float
 *   &lt;- result as Float?
 * </pre>
 * <p>
 * But a generic function could be something like this:
 * </p>
 * <pre>
 * eventHandler of type T constrain by Shape
 *   -&gt; shape as T
 *   &lt;- rtn as Boolean
 * </pre>
 * <p>
 *   In addition, a Java class that is annotated as a function <b>must</b> provide certain specific
 *   methods to be usable as an EK9 function. i.e. '_isSet()' and '._call(args...)' for example.
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9Function {
  String value();
}
