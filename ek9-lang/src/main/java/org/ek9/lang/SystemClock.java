package org.ek9.lang;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Implementation of the actual Clock.
 * This is real concrete type with actual functionality.
 */
@Ek9Class("""
    SystemClock with trait of Clock""")
public class SystemClock implements Clock {

  @Ek9Constructor("""
      SystemClock() as pure""")
  SystemClock() {
    //Just the default constructor.
  }

  @Override
  @Ek9Method("""
      override nanos() as pure
        <- rtn as Integer?""")
  public Integer nanos() {
    return Integer._of(System.nanoTime());
  }

  @Override
  @Ek9Method("""
      override millisecond() as pure
        <- rtn as Millisecond?""")
  public Millisecond millisecond() {
    return  Millisecond._of(System.currentTimeMillis());
  }

  @Override
  @Ek9Method("""
      override time() as pure
        <- rtn as Time?""")
  public Time time() {
    return Time._of(LocalTime.now());
  }

  @Override
  @Ek9Method("""
      override date() as pure
        <- rtn as Date?""")
  public Date date() {
    return Date._of(LocalDate.now());
  }

  @Override
  @Ek9Method("""
      override dateTime() as pure
        <- rtn as DateTime?""")
  public DateTime dateTime() {
    return DateTime._of(ZonedDateTime.now());
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true);
  }

}
