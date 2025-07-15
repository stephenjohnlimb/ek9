package org.ek9.lang;

import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;
import org.ek9tooling.Ek9Trait;

/**
 * Models the concept of a clock.
 * Not a concrete type but with all default implementations returning unset values.
 * So very easy to use as a mock.
 */
@Ek9Trait("""
    Clock as open""")
public interface Clock extends Any {

  @Ek9Method("""
      nanos() as pure
        <- rtn as Integer?""")
  default Integer nanos() {
    return new Integer();
  }

  @Ek9Method("""
      millisecond() as pure
        <- rtn as Millisecond?""")
  default Millisecond millisecond() {
    return new Millisecond();
  }

  @Ek9Method("""
      time() as pure
        <- rtn as Time?""")
  default Time time() {
    return new Time();
  }

  @Ek9Method("""
      date() as pure
        <- rtn as Date?""")
  default Date date() {
    return new Date();
  }

  @Ek9Method("""
      dateTime()
        <- rtn as DateTime?""")
  default DateTime dateTime() {
    return new DateTime();
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    return Boolean._of(true);
  }

}
