package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Our duration is actually a mix of period and duration period deals with years, months weeks and days
 * the duration part just deals with hour minutes and seconds.
 * So we've simplified and created some conceptual duration with a very wide range.
 */

@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Duration as open""")
public class Duration extends BuiltinType {
  private static final int SECONDS_IN_DAY = 24 * 60 * 60;
  private static final int DAYS_IN_A_MONTH = 30; //not 100% accurate but OK for dealing with durations.

  private java.time.Period thePeriod = java.time.Period.ZERO;
  private java.time.Duration theDuration = java.time.Duration.ZERO;


  @Ek9Constructor("""
      Duration() as pure
      """)
  public Duration() {
    super.unSet();
  }

  @Ek9Constructor("""
      Duration() as pure
        -> arg0 as String""")
  public Duration(String arg0) {
    unSet();
    if (arg0 != null && arg0.isSet) {
      parse(arg0.state);
    }
  }

  @Ek9Constructor("""
      Duration() as pure
        -> arg0 as Duration""")
  public Duration(Duration arg0) {
    unSet();
    assign(java.time.Period.from(arg0.thePeriod), java.time.Duration.from(arg0.theDuration));
  }


  @Ek9Method("""
      seconds() as pure
        <- rtn as Integer?""")
  public Integer seconds() {
    if (this.isSet) {
      return Integer._of(_getAsSeconds());
    }
    return new Integer();
  }

  @Ek9Method("""
      minutes() as pure
        <- rtn as Integer?""")
  public Integer minutes() {
    if (this.isSet) {
      return Integer._of(_getAsSeconds() / 60);
    }
    return new Integer();
  }

  @Ek9Method("""
      hours() as pure
        <- rtn as Integer?""")
  public Integer hours() {
    if (this.isSet) {
      return Integer._of(_getAsSeconds() / (60 * 60));
    }
    return new Integer();
  }

  @Ek9Method("""
      days() as pure
        <- rtn as Integer?""")
  public Integer days() {
    if (this.isSet) {
      return Integer._of(_getAsSeconds() / SECONDS_IN_DAY);
    }
    return new Integer();
  }

  @Ek9Method("""
      months() as pure
        <- rtn as Integer?""")
  public Integer months() {
    if (this.isSet) {
      return Integer._of(_getAsSeconds() / (DAYS_IN_A_MONTH * SECONDS_IN_DAY));
    }
    return new Integer();
  }

  @Ek9Method("""
      years() as pure
        <- rtn as Integer?""")
  public Integer years() {
    if (this.isSet) {
      return Integer._of(_getAsSeconds() / (12 * DAYS_IN_A_MONTH * SECONDS_IN_DAY));
    }
    return new Integer();
  }


  @Ek9Operator("""
      operator + as pure
        -> arg as Duration
        <- rtn as Duration?""")
  public Duration _add(Duration arg) {
    Duration rtn = _new();
    if (canProcess(arg)) {
      long totalSeconds = this._getAsSeconds() + arg._getAsSeconds();
      rtn.assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    }
    return rtn;
  }

  //TODO add in Milliseconds

  @Ek9Operator("""
      operator - as pure
        -> arg as Duration
        <- rtn as Duration?""")
  public Duration _sub(Duration arg) {
    Duration rtn = _new();
    if (canProcess(arg)) {
      long totalSeconds = this._getAsSeconds() - arg._getAsSeconds();
      rtn.assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Duration?""")
  public Duration _mul(Integer arg) {
    Duration rtn = _new();
    if (canProcess(arg)) {
      long totalSeconds = this._getAsSeconds() * arg.state;
      rtn.assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    }
    return rtn;
  }


  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Duration?""")
  public Duration _mul(Float arg) {
    Duration rtn = _new();
    if (canProcess(arg)) {
      long totalSeconds = (long) (this._getAsSeconds() * arg.state);
      rtn.assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    }
    return rtn;
  }


  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Duration?""")
  public Duration _div(Integer arg) {
    Duration rtn = _new();
    if (canProcess(arg) && arg.state != 0) {
      long totalSeconds = this._getAsSeconds() / arg.state;
      rtn.assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Duration?""")
  public Duration _div(Float arg) {
    Duration rtn = _new();
    if (canProcess(arg) && !arg._empty().state) {
      long totalSeconds = (long) (this._getAsSeconds() / arg.state);
      rtn.assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator |
        -> arg as Duration
        <- rtn as Duration?""")
  public Duration _pipe(Duration arg) {
    if (isValid(arg)) {
      if (!isSet) {
        assign(arg);
      } else {
        _addAss(arg);
      }
    }
    return this;
  }

  @Ek9Operator("""
      operator +=
        -> arg as Duration
        <- rtn as Duration?""")
  public void _addAss(Duration arg) {
    if (canProcess(arg)) {
      long totalSeconds = this._getAsSeconds() + arg._getAsSeconds();
      assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator -=
        -> arg as Duration
        <- rtn as Duration?""")
  public void _subAss(Duration arg) {
    if (canProcess(arg)) {
      long totalSeconds = this._getAsSeconds() - arg._getAsSeconds();
      assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator *=
        -> arg as Integer
        <- rtn as Duration?""")
  public void _mulAss(Integer arg) {
    if (canProcess(arg)) {
      long totalSeconds = this._getAsSeconds() * arg.state;
      assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator *=
        -> arg as Float
        <- rtn as Duration?""")
  public void _mulAss(Float arg) {
    if (canProcess(arg)) {
      long totalSeconds = (long) (this._getAsSeconds() * arg.state);
      assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator /=
        -> arg as Integer
        <- rtn as Duration?""")
  public void _divAss(Integer arg) {
    if (canProcess(arg) && arg.state != 0) {
      long totalSeconds = this._getAsSeconds() / arg.state;
      assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
      return;

    }
    unSet();
  }

  @Ek9Operator("""
      operator /=
        -> arg as Float
        <- rtn as Duration?""")
  public void _divAss(Float arg) {
    if (canProcess(arg) && !arg._empty().state) {
      long totalSeconds = (long) (this._getAsSeconds() / arg.state);
      assign(getPeriodPart(totalSeconds), getDurationPart(totalSeconds));
      return;
    }
    unSet();
  }


  @Ek9Operator("""
      operator :=:
        -> arg as Duration
        <- rtn as Duration?""")
  public void _copy(Duration arg) {
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
      operator <=> as pure
        -> arg as Duration
        <- rtn as Integer?""")
  public Integer _cmp(Duration arg) {
    if (canProcess(arg)) {
      return Integer._of(compare(arg));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Duration asDuration) {
      return _cmp(asDuration);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Duration
        <- rtn as Boolean?""")
  public Boolean _lt(Duration arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) < 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator <= as pure
        -> arg as Duration
        <- rtn as Boolean?""")
  public Boolean _lteq(Duration arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) <= 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator > as pure
        -> arg as Duration
        <- rtn as Boolean?""")
  public Boolean _gt(Duration arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) > 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator >= as pure
        -> arg as Duration
        <- rtn as Boolean?""")
  public Boolean _gteq(Duration arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) >= 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator == as pure
        -> arg as Duration
        <- rtn as Boolean?""")
  public Boolean _eq(Duration arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) == 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator <> as pure
        -> arg as Duration
        <- rtn as Boolean?""")
  public Boolean _neq(Duration arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) != 0);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator - as pure
        <- rtn as Duration?""")
  public Duration _negate() {
    Duration rtn = _new();
    rtn.assign(this.thePeriod.negated(), this.theDuration.negated());
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(toString());
    }
    return new String();
  }

  //Start of Utility methods.

  private int compare(Duration arg) {
    if (this.thePeriod.getDays() < arg.thePeriod.getDays()) {
      return -1;
    }
    if (this.thePeriod.getDays() > arg.thePeriod.getDays()) {
      return 1;
    }

    final var durationCompareResult = java.lang.Integer.compare(this.theDuration.compareTo(arg.theDuration), 0);
    if (durationCompareResult < 0) {
      return -1;
    } else if (durationCompareResult > 0) {
      return 1;
    }
    return 0;
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private void parse(java.lang.String value) {
    //Need to check if starts with a '-' - which means the overall value is minus
    //But note you can still use '-' with parts of the duration
    // For example "-PT-6H+3M"  -- parses as "+6 hours and -3 minutes"!!

    final var negative = value.startsWith("-");

    java.lang.String[] split = value.split("T");

    try {
      //See if there is a period part.
      final var expectLength = negative ? 2 : 1;
      java.time.Period p = java.time.Period.ZERO;
      java.time.Duration d = java.time.Duration.ZERO;
      if (split[0].length() > expectLength) {
        //This will include the '-' before the P and any in the years months days.
        p = java.time.Period.parse(split[0]);
      }

      //now make a valid duration in java terms.
      if (split.length > 1) {
        //If overall was negative then need to ensure the duration part also is negative
        //But there can be sub elements that are also negative. (see above).
        final var localPrefix = negative ? "-PT" : "PT";
        d = java.time.Duration.parse(localPrefix + split[1]);
      }
      assign(p, d);
    } catch (RuntimeException _) {
      //Leave as unset.
    }
  }

  private void assign(Duration value) {
    if (value.isSet) {
      assign(java.time.Period.from(value.thePeriod), java.time.Duration.from(value.theDuration));
    }
  }

  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  private void assign(java.time.Period p, java.time.Duration d) {
    java.time.Period periodBefore = this.thePeriod;
    java.time.Duration durationBefore = this.theDuration;
    boolean beforeIsValid = isSet;

    thePeriod = p;
    theDuration = d;
    set();
    if (!validateConstraints().state) {
      java.lang.String stringTo = this.toString();
      thePeriod = periodBefore;
      theDuration = durationBefore;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + this + " to " + stringTo);
    }

  }

  @Override
  public int hashCode() {
    return theDuration.hashCode() + thePeriod.hashCode();
  }

  @Override

  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof Duration duration) {
      if (isSet) {
        return theDuration.equals(duration.theDuration) && thePeriod.equals(duration.thePeriod);
      }
      return true;
    }
    return false;
  }

  @Override
  public java.lang.String toString() {
    if (isSet) {
      if (thePeriod.isZero() && theDuration.isZero()) {
        return "PT0S";
      }
      //make one duration string from the two. need to drop the 'P' prefix of the time.
      StringBuilder rtn = new StringBuilder("P");
      if (!thePeriod.isZero()) {
        rtn.append(thePeriod.toString().substring(1));
      }
      if (!theDuration.isZero()) {
        rtn.append(theDuration.toString().substring(1));
      }
      return rtn.toString();
    }
    return "";
  }

  long _getAsSeconds() {
    long rtn = this.theDuration.toSeconds();
    rtn += (long) this.thePeriod.getDays() * SECONDS_IN_DAY;

    return rtn;
  }

  private static java.time.Duration getDurationPart(long fromSeconds) {
    long days = fromSeconds / SECONDS_IN_DAY;
    long remainingSeconds = fromSeconds - days * SECONDS_IN_DAY;
    return java.time.Duration.ofSeconds(remainingSeconds);
  }

  private static java.time.Period getPeriodPart(long fromSeconds) {
    int days = (int) (fromSeconds / SECONDS_IN_DAY);

    int months = days / DAYS_IN_A_MONTH;
    days = days - months * DAYS_IN_A_MONTH;

    final int monthsInAYear = 12;
    int years = months / monthsInAYear;
    months = months - years * monthsInAYear;

    return java.time.Period.of(years, months, days);
  }

  protected Duration _new() {
    return new Duration();
  }

  public static Duration _of(java.time.Duration diff) {
    Duration rtn = new Duration();
    rtn.assign(java.time.Period.ZERO, diff);
    return rtn;
  }

  public static Duration _of(java.time.Duration diff, java.time.Period diff2) {
    Duration rtn = new Duration();
    rtn.assign(diff2, diff);
    return rtn;
  }

  public static Duration _of(java.lang.String value) {
    Duration rtn = new Duration();
    rtn.parse(value);

    return rtn;
  }

  public static Duration _of(long fromSeconds) {
    Duration rtn = new Duration();
    rtn.assign(getPeriodPart(fromSeconds), getDurationPart(fromSeconds));
    return rtn;
  }

}

