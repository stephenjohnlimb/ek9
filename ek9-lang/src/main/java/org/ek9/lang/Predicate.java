package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts one argument of type T and returns a value of type Boolean.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    Predicate of type T as pure abstract
      -> t as T
      <- r as Boolean?""")
public class Predicate implements Any {

  @Ek9Constructor("Predicate() as pure")
  public Predicate() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a Predicate to implement this.

  public Boolean _call(Any t) {
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }

}
