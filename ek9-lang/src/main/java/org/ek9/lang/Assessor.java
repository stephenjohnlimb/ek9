package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts one argument of type T and returns a value of type Boolean.
 * Like a Predicate but not pure in nature.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    Assessor of type T as abstract
      -> t as T
      <- r as Boolean?""")
public class Assessor implements Any {

  @Ek9Constructor("Assessor() as pure")
  public Assessor() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a Assessor to implement this.

  public Boolean _call(Any t) {
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }

}
