package org.ek9.lang;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Date time class with Epoch is 01 01 1970 00:00:00 Z.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    DateTime as open""")
public class DateTime extends BuiltinType implements TemporalItem {
  java.time.ZonedDateTime state = java.time.ZonedDateTime.now().withNano(0);


  @SuppressWarnings("checkstyle:CatchParameterName")
  public static DateTime _of(java.lang.String value) {
    DateTime rtn = new DateTime();
    try {
      rtn.assign(java.time.ZonedDateTime.parse(value));
    } catch (RuntimeException _) {
      //ignore and leave rtn unset
    }

    return rtn;
  }

  public static DateTime _of(java.time.ZonedDateTime value) {
    DateTime rtn = new DateTime();
    rtn.assign(java.time.ZonedDateTime.parse(value.toString()));
    return rtn;
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static DateTime _ofHttpDateTime(java.lang.String httpDateTime) {
    DateTime rtn = new DateTime();
    try {
      TemporalAccessor result = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O").parse(httpDateTime);
      rtn.assign(ZonedDateTime.from(result));
    } catch (Exception _) {
      //ignore and leave rtn unset

    }
    return rtn;
  }


  @Ek9Constructor("""
      DateTime() as pure""")
  public DateTime() {
    super.unSet();
  }


  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Constructor("""
      DateTime() as pure
        -> arg0 as String""")
  public DateTime(String value) {
    unSet();
    if (isValid(value)) {
      try {
        assign(java.time.ZonedDateTime.parse(value.state));
      } catch (Exception _) {
        //ignore and leave rtn unset
      }
    }
  }

  @Ek9Constructor("""
      DateTime() as pure
        -> arg0 as DateTime""")
  public DateTime(DateTime value) {
    unSet();
    if (isValid(value)) {
      assign(value.state);
    }
  }


  @Ek9Constructor("""
      DateTime() as pure
        -> arg0 as Date""")
  public DateTime(Date date) {
    unSet();
    if (isValid(date)) {
      assign(
          java.time.ZonedDateTime.of((int) date.year().state, (int) date.month().state, (int) date.day().state, 0, 0, 0,
              0, ZoneId.of("Z")));
    }
  }


  @Ek9Constructor("""
      DateTime() as pure
        ->
          year as Integer
          month as Integer
          dayOfMonth as Integer""")
  public DateTime(Integer year, Integer month, Integer dayOfMonth) {
    unSet();
    if (isValid(year) && isValid(month) && isValid(dayOfMonth)) {
      assign(java.time.ZonedDateTime.of((int) year.state, (int) month.state, (int) dayOfMonth.state, 0, 0, 0, 0,
          ZoneId.of("Z")));
    }
  }

  @Ek9Constructor("""
      DateTime() as pure
        ->
          year as Integer
          month as Integer
          dayOfMonth as Integer
          hour as Integer""")
  public DateTime(Integer year, Integer month, Integer dayOfMonth, Integer hour) {
    unSet();
    if (isValid(year) && isValid(month) && isValid(dayOfMonth) && isValid(hour)) {
      assign(
          java.time.ZonedDateTime.of((int) year.state, (int) month.state, (int) dayOfMonth.state, (int) hour.state, 0,
              0, 0, ZoneId.of("Z")));
    }
  }


  @Ek9Constructor("""
      DateTime() as pure
        ->
          year as Integer
          month as Integer
          dayOfMonth as Integer
          hour as Integer
          minute as Integer""")
  public DateTime(Integer year, Integer month, Integer dayOfMonth, Integer hour, Integer minute) {
    unSet();
    if (isValid(year) && isValid(month) && isValid(dayOfMonth) && isValid(hour) && isValid(minute)) {
      assign(java.time.ZonedDateTime.of((int) year.state, (int) month.state, (int) dayOfMonth.state, (int) hour.state,
          (int) minute.state, 0, 0, ZoneId.of("Z")));
    }
  }

  @Ek9Constructor("""
      DateTime() as pure
        ->
          year as Integer
          month as Integer
          dayOfMonth as Integer
          hour as Integer
          minute as Integer
          second as Integer""")
  public DateTime(Integer year, Integer month, Integer dayOfMonth, Integer hour, Integer minute, Integer second) {
    unSet();
    if (isValid(year) && isValid(month) && isValid(dayOfMonth) && isValid(hour) && isValid(minute) && isValid(second)) {
      assign(java.time.ZonedDateTime.of((int) year.state, (int) month.state, (int) dayOfMonth.state, (int) hour.state,
          (int) minute.state, (int) second.state, 0, ZoneId.of("Z")));
    }
  }


  @Ek9Method("""
      today() as pure
        <- rtn as DateTime?""")
  public DateTime today() {
    DateTime rtn = _new();
    rtn.setToday();
    return rtn;
  }


  @Ek9Method("""
      now() as pure
        <- rtn as DateTime?""")
  public DateTime now() {
    return today();
  }

  @Ek9Method("setToday()")
  public void setToday() {
    assign(java.time.ZonedDateTime.now().withNano(0));
  }

  @Ek9Method("""
      clear()
        <- rtn as DateTime?""")
  public DateTime clear() {
    this.state = java.time.ZonedDateTime.now().withNano(0);
    super.unSet();
    return this;
  }


  @Override
  public java.time.ZonedDateTime _getAsJavaTemporalAccessor() {
    return this.state;
  }


  @Ek9Method("""
      withSameInstant() as pure
        -> zoneId as String
        <- rtn as DateTime?""")
  public DateTime withSameInstant(String zoneId) {
    DateTime rtn = _new();
    if (canProcess(zoneId)) {
      rtn.assign(state.withZoneSameInstant(ZoneId.of(zoneId.state)));
    }
    return rtn;
  }

  @Ek9Method("""
      withZone() as pure
        -> zoneId as String
        <- rtn as DateTime?""")
  public DateTime withZone(String zoneId) {
    DateTime rtn = _new();
    if (canProcess(zoneId)) {
      rtn.assign(state.withZoneSameLocal(ZoneId.of(zoneId.state)));
    }
    return rtn;
  }

  //TODO duration additions

  //TODO MIllisecond additions.

  //TODO subtraction of date time to give duration

  @Ek9Operator("""
      operator |
        -> arg as DateTime
        <- rtn as DateTime?""")
  public DateTime _pipe(DateTime value) {
    //If valid we just assign to the value overwriting what was there
    if (isValid(value)) {
      assign(value.state);
    }
    return this;
  }

  //TODO pipe duration

  @Ek9Method("""
      year() as pure
        <- rtn as Integer?""")
  public Integer year() {
    if (isSet) {
      return Integer._of(state.getYear());
    }
    return new Integer();
  }

  @Ek9Method("""
      month() as pure
        <- rtn as Integer?""")
  public Integer month() {
    if (isSet) {
      return Integer._of(state.getMonthValue());
    }
    return new Integer();
  }

  @Ek9Method("""
      day() as pure
        <- rtn as Integer?""")
  public Integer day() {
    if (isSet) {
      return Integer._of(state.getDayOfMonth());
    }
    return new Integer();
  }

  /**
   * 1 is normally a Monday 7 a Sunday but this will vary based on localisation.
   */
  @Ek9Method("""
      dayOfWeek() as pure
        <- rtn as Integer?""")
  public Integer dayOfWeek() {
    if (isSet) {
      return Integer._of(state.getDayOfWeek().getValue());
    }
    return new Integer();
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

  @Ek9Method("""
      zone() as pure
        <- rtn as String?""")
  public String zone() {
    if (isSet) {
      return String._of(state.getZone().toString());
    }
    return new String();
  }

  //TODO duration offset from UTC

  @Ek9Operator("""
      operator <=> as pure
        -> arg as DateTime
        <- rtn as Integer?""")
  public Integer _cmp(DateTime arg) {
    if (canProcess(arg)) {
      return Integer._of(this.state.compareTo(arg.state));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as DateTime
        <- rtn as Boolean?""")
  public Boolean _neq(DateTime arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.compareTo(arg.state) != 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as DateTime
        <- rtn as Boolean?""")
  public Boolean _eq(DateTime arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.isEqual(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as DateTime
        <- rtn as Boolean?""")
  public Boolean _lt(DateTime arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isBefore(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as DateTime
        <- rtn as Boolean?""")
  public Boolean _lteq(DateTime arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isBefore(arg.state) || this.state.isEqual(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as DateTime
        <- rtn as Boolean?""")
  public Boolean _gt(DateTime arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isAfter(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as DateTime
        <- rtn as Boolean?""")
  public Boolean _gteq(DateTime arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isAfter(arg.state) || this.state.isEqual(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator :=:
        -> arg as DateTime
        <- rtn as DateTime?""")
  public void _copy(DateTime arg) {
    if (isValid(arg)) {
      assign(arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    if (isSet) {
      return String._of(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.state));
    }
    return new String();
  }

  @Ek9Method("""
      rfc7231() as pure
        <- rtn as String?""")
  public String rfc7231() {
    if (isSet) {
      return String._of(DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss O").format(this.state));
    }
    return new String();
  }

  //Start of Utility methods

  protected DateTime _new() {
    return new DateTime();
  }

  private void assign(java.time.ZonedDateTime value) {
    java.time.ZonedDateTime before = state;
    state = value;
    if (!validateConstraints().state) {
      state = before;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }
    isSet = true;
  }

  @Override
  public int hashCode() {
    return state.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof DateTime dateTime && isSet) {
      return state.equals(dateTime.state);
    }

    return false;
  }

  @Override
  public java.lang.String toString() {
    if (isSet) {
      return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(this.state);
    }
    return "";
  }
}
