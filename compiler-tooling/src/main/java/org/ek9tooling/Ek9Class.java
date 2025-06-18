package org.ek9tooling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To be used to annotate a Java class that can be exposed as an EK9 class.
 * Note that it is possible to provide a value here for generic classes as the name of the
 * class will need more EK9 decoration.
 * <p>
 * For example as simple class:
 * </p>
 * <pre>
 * Coordinate
 * </pre>
 * <p>
 * But a generic class could be something like this:
 * </p>
 * <pre>
 * List of type L
 * </pre>
 * <p>
 *   But note if the Java library is also providing classes that implement 'List'.
 *   Then they would result in a decorated class name but would also need to be defined as in EK9
 *   parameterised generic types are new real types (unlike Java which just become List&lt;Object&gt;.
 * </p>
 * <p>
 *   So it would be necessary to define that new type with the correct decorated name <b>and</b> provide the
 *   correct EK9 name. i.e.<br/>
 *   'List of String'<br/>
 *   would be 'mangled' to:<br>
 *   '_List_8F118296CF271EAEB58F9D4B4FDDDB2DA7B80C13BF342D8C4A916D54EBB208E1'
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)

public @interface Ek9Class {
  String value() default "className";
}
