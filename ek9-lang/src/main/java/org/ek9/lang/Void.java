package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * Nothing, just void.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class
public class Void extends BuiltinType {

  @Ek9Constructor("Void() as pure")
  public Void() {
    //Does nothing and is not set.
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(false);
  }

  //Start of Utility methods.
  @Override
  protected BuiltinType _new() {
    return new Void();
  }
}
