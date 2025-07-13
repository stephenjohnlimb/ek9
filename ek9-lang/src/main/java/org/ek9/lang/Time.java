package org.ek9.lang;

import java.time.LocalTime;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;


/**
 * '00:00' is the start of the day.
 * '23:59:59' is the end of the day.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Time as open""")
public class Time extends BuiltinType implements TemporalItem {
  java.time.LocalTime state = java.time.LocalTime.now();


  @Ek9Constructor("Time() as pure")
  public Time() {
    super.unSet();
  }

  @Ek9Constructor("""
      Time() as pure
        -> arg0 as Time""")
  public Time(Time arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state);
    }
  }

  /**
   * Create a new time based on the number of seconds into the day.
   */
  @Ek9Constructor("""
      Time() as pure
        -> secondOfDay as Integer""")
  public Time(Integer secondOfDay) {
    unSet();
    if (isValid(secondOfDay)) {
      LocalTime proposed;
      try {
        proposed = LocalTime.ofSecondOfDay(secondOfDay.state);
      } catch (java.lang.Exception ex) {
        throw new RuntimeException("Constraint violation invalid time", ex);
      }
      assign(proposed);
    }
  }

  @Ek9Constructor("""
      Time() as pure
        ->
          hour as Integer
          minute as Integer""")
  public Time(Integer hour, Integer minute) {
    unSet();
    if (isValid(hour) && isValid(minute)) {
      LocalTime proposed;
      try {
        proposed = LocalTime.of((int) hour.state, (int) minute.state);
      } catch (java.lang.Exception ex) {
        throw new RuntimeException("Constraint violation invalid time", ex);
      }
      assign(proposed);
    }
  }


  @Ek9Constructor("""
      Time() as pure
        ->
          hour as Integer
          minute as Integer
          second as Integer""")
  public Time(Integer hour, Integer minute, Integer second) {
    unSet();
    if (isValid(hour) && isValid(minute) && isValid(second)) {
      LocalTime proposed;
      try {
        proposed = LocalTime.of((int) hour.state, (int) minute.state, (int) second.state);
      } catch (java.lang.Exception ex) {
        throw new RuntimeException("Constraint violation invalid time", ex);
      }
      assign(proposed);
    }
  }

  @Ek9Constructor("""
      Time() as pure
        -> arg0 as String""")
  public Time(String arg0) {
    if (arg0 != null && arg0.isSet) {
      assign(Time._of(arg0.state));
    }
  }

  @Ek9Method("""
      clear()
        <- rtn as Time?""")
  public Time clear() {
    this.state = LocalTime.MIN;
    super.unSet();
    return this;
  }

  @Ek9Method("""
      startOfDay() as pure
        <- rtn as Time?""")
  public Time startOfDay() {
    Time rtn = _new();
    rtn.setStartOfDay();
    return rtn;
  }

  @Ek9Method("""
      endOfDay() as pure
        <- rtn as Time?""")
  public Time endOfDay() {
    Time rtn = _new();
    rtn.setEndOfDay();
    return rtn;
  }

  @Ek9Method("""
      set()
        -> toSet as Time""")
  public void set(Time toSet) {
    assign(toSet);
  }

  @Ek9Method("setStartOfDay()")
  public void setStartOfDay() {
    assign(LocalTime.MIN);
  }

  @Ek9Method("setEndOfDay()")
  public void setEndOfDay() {
    assign(LocalTime.MAX);
  }

  //TODO add in durations

  //TODO add in milliseconds

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Integer?""")
  public Integer _prefix() {
    return hour();
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as Integer?""")
  public Integer _suffix() {
    return second();
  }

  @Ek9Method("""
      hour() as pure
        <- rtn as Integer?""")
  public Integer hour() {
    if (isSet) {
      return Integer._of(state.getHour());
    }
    return new Integer();
  }

  @Ek9Method("""
      minute() as pure
        <- rtn as Integer?""")
  public Integer minute() {
    if (isSet) {
      return Integer._of(state.getMinute());
    }
    return new Integer();
  }

  @Ek9Method("""
      second() as pure
        <- rtn as Integer?""")
  public Integer second() {
    if (isSet) {
      return Integer._of(state.getSecond());
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Time
        <- rtn as Integer?""")
  public Integer _cmp(Time arg) {
    if (canProcess(arg)) {
      return Integer._of(this.state.compareTo(arg.state));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Time asTime) {
      return _cmp(asTime);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Time
        <- rtn as Boolean?""")
  public Boolean _lt(Time arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isBefore(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Time
        <- rtn as Boolean?""")
  public Boolean _lteq(Time arg) {
    if (canProcess(arg)) {
      return Boolean._of(!this.state.isAfter(arg.state));
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator > as pure
        -> arg as Time
        <- rtn as Boolean?""")
  public Boolean _gt(Time arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isAfter(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Time
        <- rtn as Boolean?""")
  public Boolean _gteq(Time arg) {
    if (canProcess(arg)) {
      return Boolean._of(!this.state.isBefore(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Time
        <- rtn as Boolean?""")
  public Boolean _eq(Time arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Time
        <- rtn as Boolean?""")
  public Boolean _neq(Time arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Time""")
  public void _copy(Time arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      super.unSet();
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    if (isSet) {
      StringBuilder buffer = new StringBuilder();
      int hourValue = state.getHour();
      int minuteValue = state.getMinute();
      int secondValue = state.getSecond();
      buffer.append(hourValue < 10 ? "0" : "").append(hourValue);
      buffer.append(minuteValue < 10 ? ":0" : ":").append(minuteValue);
      buffer.append(secondValue < 10 ? ":0" : ":").append(secondValue);
      return String._of(buffer.toString());
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(state.hashCode());
    }
    return rtn;
  }

  //Start of Utility methods

  @Override
  public java.time.LocalTime _getAsJavaTemporalAccessor() {
    return this.state;
  }

  private void assign(Time value) {
    if (isValid(value)) {
      assign(value.state);
    } else {
      unSet();
    }
  }

  public void assign(java.time.LocalTime value) {
    java.time.LocalTime before = state;
    boolean beforeIsValid = isSet;
    state = value;
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }
  }

  protected Time _new() {
    return new Time();
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static Time _of(java.lang.String value) {
    Time rtn = new Time();
    try {
      rtn.assign(java.time.LocalTime.parse(value));
    } catch (java.lang.Exception _) {
      //Leave as unset.
    }

    return rtn;
  }

  public static Time _of(java.time.LocalTime value) {
    Time rtn = new Time();
    rtn.assign(value);
    return rtn;
  }

}

