package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * This is a template function type, that basically accepts an object (but is not pure in nature).
 * Here, it is defined with 'Any'.
 * When the Consumer is parameterised with a real type the T will be replaced by that type.
 * Hence, 'call(t as Any)', will become 'call(t as Integer)'.
 * If Consumer was parameterised with an Integer.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    Acceptor of type T as abstract
      -> t as T""")
public class Acceptor implements Any {

  @Ek9Constructor("Acceptor() as pure")
  public Acceptor() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' an Acceptor to implement this.

  public void _call(Any t) {
    //No op in this as it is Ek9 abstract.
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }


}
