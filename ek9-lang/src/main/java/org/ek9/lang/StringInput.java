package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * StringInput use for various mechanism that can produce Strings.
 * For example TextFile and Stdin.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Trait("""
    StringInput with trait of IO as open""")
public interface StringInput extends IO {

  @Ek9Method("""
      next() as pure
        <- rtn as String?""")
  default String next() {
    return new String();
  }

  @Ek9Method("""
      hasNext() as pure
        <- rtn as Boolean?""")
  default Boolean hasNext() {
    return new Boolean();
  }

  @Ek9Operator("""
      operator close as pure""")
  default void _close() {
    //By default a 'no-op'.
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    //Not sure if set or not, implementation must define.
    return new Boolean();
  }
}
