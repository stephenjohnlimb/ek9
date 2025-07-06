package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * This is a template function type, that basically supplies an object (but is not pure in nature).
 * Here, it is defined with 'Any'.
 * When the Producer is parameterised with a real type the T will be replaced by that type.
 * Hence, 'call() <- r as Any', will become 'call() r <- Integer'.
 * If Producer was parameterised with an Integer.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    Producer of type T as abstract
      <- r as T?""")
public class Producer implements Any {

  @Ek9Constructor("Producer() as pure")
  public Producer() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a Producer to implement this.

  public Any _call() {
    return new Any() {
    };
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }


}
