package org.ek9.lang;

import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * This is a marker trait for any type that typically marks methods/operators are 'pure',
 * but actually does have side effects like IO.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Trait("""
    IO as open""")
public interface IO extends Any {

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    //Not sure if set or not, implementation must define.
    return new Boolean();
  }
}
