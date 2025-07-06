package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts one argument of type T and returns a value also of type T.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    UnaryOperator of type T as pure abstract
      -> t as T
      <- r as T?""")
public class UnaryOperator implements Any {

  @Ek9Constructor("UnaryOperator() as pure")
  public UnaryOperator() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a UnaryOperator to implement this.

  public Any _call(Any t) {
    return t;
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }


}
