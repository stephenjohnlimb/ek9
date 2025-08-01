package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts arguments of type T, U and returns a value of type R, but not pure in nature.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    BiRoutine of type (T, U, R) as abstract
      ->
        t as T
        u as U
      <-
        r as R?""")
public class BiRoutine implements Any {

  @Ek9Constructor("BiRoutine() as pure")
  public BiRoutine() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a BiRoutine to implement this.

  public Any _call(Any t, Any u) {
    return new Any() {};
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }

}
