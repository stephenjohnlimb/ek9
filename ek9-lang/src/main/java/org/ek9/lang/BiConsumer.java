package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * This is a template function type, that basically consumes two objects (and is pure in nature).
 * Here, it is defined with 'Any'.
 * When the Consumer is parameterised with a real type the T and U will be replaced by that type.
 * Hence, 'call(t as Any, u as Any), will become 'call(t as Integer, u as String)'.
 * If Consumer was parameterised with an Integer and a String.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    BiConsumer of type (T, U) as pure abstract
      ->
        t as T
        u as U""")
public class BiConsumer implements Any {

  @Ek9Constructor("BiConsumer() as pure")
  public BiConsumer() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a BiConsumer to implement this.

  public void _call(Any t, Any u) {
    //No op in this as it is Ek9 abstract.
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }


}
