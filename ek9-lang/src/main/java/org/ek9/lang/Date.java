package org.ek9.lang;

import java.time.DateTimeException;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Date class, uses Epoch as 01 01 1970.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Date as open""")
public class Date extends BuiltinType implements TemporalItem {

  java.time.LocalDate state = java.time.LocalDate.now();

  @Ek9Constructor("""
      Date() as pure""")
  public Date() {
    super.unSet();
  }

  @Ek9Constructor("""
      Date() as pure
        -> arg0 as String""")
  public Date(String arg0) {
    unSet();
    if (isValid(arg0)) {
      parse(arg0.state).ifPresent(this::assign);
    }
  }

  @Ek9Constructor("""
      Date() as pure
        -> arg0 as Date""")
  public Date(Date arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state);
    }
  }

  @Ek9Constructor("""
      Date() as pure
        -> daysFromEpoch as Integer""")
  public Date(Integer daysFromEpoch) {
    unSet();
    if (isValid(daysFromEpoch)) {
      assign(java.time.LocalDate.ofEpochDay(daysFromEpoch.state));
    }
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Constructor("""
      Date() as pure
        ->
          year as Integer
          month as Integer
          dayOfMonth as Integer""")
  public Date(Integer year, Integer month, Integer dayOfMonth) {
    unSet();
    if (isValid(year) && isValid(month) && isValid(dayOfMonth)) {
      try {
        assign(java.time.LocalDate.of((int) year.state, (int) month.state, (int) dayOfMonth.state));
      } catch (DateTimeException _) {
        //Leave as unset.
      }
    }
  }

  @Ek9Method("""
      today() as pure
        <- rtn as Date?""")
  public Date today() {
    Date rtn = _new();
    rtn.setToday();
    return rtn;
  }

  @Ek9Method("""
      setToday()""")
  public void setToday() {
    assign(java.time.LocalDate.now());
  }

  @Ek9Method("""
      clear()
        <- rtn as Date?""")
  public Date clear() {
    this.state = java.time.LocalDate.now();
    super.unSet();
    return this;
  }


  //TODO milliseconds
  //TODO durations

  @Ek9Operator("""
      operator |
        -> arg as Date""")
  public void _pipe(Date value) {
    //We can only overwrite if piping in dates. not additive
    if (isValid(value)) {
      assign(value.state);
    }
  }

  //TODO pipe in Duration.

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

  @Ek9Method("""
      dayOfYear() as pure
        <- rtn as Integer?""")
  public Integer dayOfYear() {
    if (isSet) {
      return Integer._of(state.getDayOfYear());
    }
    return new Integer();
  }

  @Ek9Method("""
      dayOfMonth() as pure
        <- rtn as Integer?""")
  public Integer dayOfMonth() {
    return day();
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


  @Ek9Operator("""
      operator < as pure
        -> arg as Date
        <- rtn as Boolean?""")
  public Boolean _lt(Date arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isBefore(arg.state));
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator <= as pure
        -> arg as Date
        <- rtn as Boolean?""")
  public Boolean _lteq(Date arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isBefore(arg.state) || this.state.isEqual(arg.state));
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator > as pure
        -> arg as Date
        <- rtn as Boolean?""")
  public Boolean _gt(Date arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isAfter(arg.state));
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator >= as pure
        -> arg as Date
        <- rtn as Boolean?""")
  public Boolean _gteq(Date arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state.isAfter(arg.state) || this.state.isEqual(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Date
        <- rtn as Boolean?""")
  public Boolean _neq(Date arg) {
    if (canProcess(arg)) {
      return Boolean._of(!this.state.isEqual(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Date
        <- rtn as Boolean?""")
  public Boolean _eq(Date arg) {
    if (canProcess(arg)) {
      return Boolean._of(state.isEqual(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Date
        <- rtn as Integer?""")
  public Integer _cmp(Date arg) {
    if (canProcess(arg)) {
      //We don't want the number (as it changes in implementation) just eq lt or gt.
      final var cmpResult = this.state.compareTo(arg.state);
      if (cmpResult == 0) {
        return Integer._of(0);
      }
      if (cmpResult > 0) {
        return Integer._of(1);
      }
      return Integer._of(-1);

    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Date asDate) {
      return _cmp(asDate);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Date
        <- rtn as Integer?""")
  public Integer _fuzzy(Date arg) {
    return _cmp(arg);
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(state.toString());
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

  //TODO prefix and suffix

  @Ek9Operator("""
      operator :~:
        -> arg as Date""")
  public void _merge(Date arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Date""")
  public void _replace(Date arg) {
    _copy(arg);
  }


  @Ek9Operator("""
      operator :=:
        -> arg as Date""")
  public void _copy(Date arg) {
    if (isValid(arg)) {
      assign(arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator ++
        <- rtn as Date?""")
  public Date _inc() {
    if (isSet) {
      assign(state.plusDays(1));
    }
    return this;
  }

  @Ek9Operator("""
      operator --
        <- rtn as Date?""")
  public Date _dec() {
    if (isSet) {
      assign(state.minusDays(1));
    }
    return this;
  }

  //Start of Utility methods

  private void assign(java.time.LocalDate value) {
    java.time.LocalDate before = state;
    boolean beforeIsValid = isSet;
    state = value;
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }
  }

  protected Date _new() {
    return new Date();
  }

  @Override
  public java.time.LocalDate _getAsJavaTemporalAccessor() {
    return this.state;
  }

  public static Date _of(java.time.LocalDate value) {
    Date rtn = new Date();
    //make fresh copy
    parse(value.toString()).ifPresent(rtn::assign);
    return rtn;
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static Date _of(java.lang.String value) {
    Date rtn = new Date();
    parse(value).ifPresent(rtn::assign);
    return rtn;
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private static java.util.Optional<java.time.LocalDate> parse(java.lang.String arg) {
    try {
      return java.util.Optional.of(java.time.LocalDate.parse(arg));
    } catch (RuntimeException _) {
      //ignore and leave rtn unset
    }
    return java.util.Optional.empty();
  }

}
