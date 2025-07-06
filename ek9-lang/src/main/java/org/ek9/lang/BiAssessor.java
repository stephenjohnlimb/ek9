package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Accepts arguments of type T, U and returns a value of type Boolean.
 * This is not pure in nature, i.e. can mutate state.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    BiAssessor of type (T, U) as abstract
      ->
        t as T
        u as U
      <-
        r as Boolean?""")
public class BiAssessor implements Any {

  @Ek9Constructor("BiAssessor() as pure")
  public BiAssessor() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a BiAssessor to implement this.

  public Boolean _call(Any t, Any u) {
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
