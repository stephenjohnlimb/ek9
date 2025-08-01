package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts two arguments of type T and returns a value of type Integer.
 * i.e. it is pure (cannot mutate state) and is a comparator.
 * returns 0 is t1 and t2 are equal, -1 is t1 is less than t2 and +1 is t1 is greater than t2.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    Comparator of type T as pure abstract
      ->
        t1 as T
        t2 as T
      <-
        r as Integer?""")
public class Comparator implements Any {

  @Ek9Constructor("Comparator() as pure")
  public Comparator() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a Comparator to implement this.

  public Integer _call(Any t, Any u) {
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    //This is set but will result return an unset Integer.
    return Boolean._of(true);
  }

}
