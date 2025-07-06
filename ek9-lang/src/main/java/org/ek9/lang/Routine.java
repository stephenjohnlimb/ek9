package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts one argument of type T and returns a value of type R, but is not pure.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    Routine of type (T, R) as abstract
      -> t as T
      <- r as R?""")
public class Routine implements Any {

  @Ek9Constructor("Routine() as pure")
  public Routine() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a Routine to implement this.

  public Any _call(Any t) {
    return new Any() {};
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }

}
