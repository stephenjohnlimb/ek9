package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * StringOutput use for various mechanisms that can consume Strings.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Trait("""
     StringOutput with trait of IO as open""")
public interface StringOutput extends IO {

  @Ek9Method("""
      println() as pure
        -> arg0 as String""")
  default void println(String arg0) {
    //By default a 'no-op'.
  }

  @Ek9Method("""
      println() as pure
        -> arg0 as Any""")
  default void println(Any arg0) {
    if (canProcess(arg0)) {
      println(arg0._string());
    } //else ignore it.
  }

  @Ek9Method("""
      print() as pure
        -> arg0 as String""")
  default void print(String arg0) {
    //By default a 'no-op'.
  }

  @Ek9Method("""
      print() as pure
        -> arg0 as Any""")
  default void print(Any arg0) {
    if (canProcess(arg0)) {
      print(arg0._string());
    } //else ignore it.
  }

  @Ek9Operator("""
      operator |
        -> arg0 as String""")
  default void _pipe(String arg0) {
    println(arg0);
  }

  @Ek9Operator("""
      operator |
        -> arg0 as Any""")
  default void _pipe(Any arg0) {
    if (canProcess(arg0)) {
      println(arg0._string());
    } //else ignore it.
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
