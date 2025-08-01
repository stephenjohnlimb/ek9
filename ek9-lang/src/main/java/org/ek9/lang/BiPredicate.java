package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts arguments of type T, U and returns a value of type Boolean.
 * This is pure in nature, cannot mutate data.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    BiPredicate of type (T, U) as pure abstract
      ->
        t as T
        u as U
      <-
        r as Boolean?""")
public class BiPredicate implements Any {

  @Ek9Constructor("BiPredicate() as pure")
  public BiPredicate() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a BiPredicate to implement this.

  public Boolean _call(Any t, Any u) {
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
