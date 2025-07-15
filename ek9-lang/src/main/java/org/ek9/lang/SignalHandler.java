package org.ek9.lang;

import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Function;
import org.ek9tooling.Ek9Operator;

/**
 * Used with signals for callbacks.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Function("""
    SignalHandler() as abstract
      -> signal as String
      <- result as Integer?""")
public class SignalHandler implements Any {

  @Ek9Constructor("SignalHandler() as pure")
  public SignalHandler() {
    //Default constructor - not exposed in any way to EK9
  }

  //Expect that class (actually function) that 'IS' a Function to implement this.

  public Integer _call(String signal) {
    return new Integer() {
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
