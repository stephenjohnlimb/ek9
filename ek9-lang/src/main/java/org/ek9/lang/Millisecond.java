package org.ek9.lang;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Definition of Milliseconds.
 * Useful for timeouts and other fairly short durations of time.
 * But, strongly typed.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Millisecond as open""")
public class Millisecond extends SuffixedComponent {

  long state = 0;

  @Ek9Constructor("""
      Millisecond() as pure""")
  public Millisecond() {
    super.unSet();
  }

  @Ek9Constructor("""
      Millisecond() as pure
        -> arg0 as Duration""")
  public Millisecond(Duration arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0._getAsSeconds() * 1000, "ms");
    }
  }

  @Ek9Constructor("""
      Millisecond() as pure
        -> arg0 as Millisecond""")
  public Millisecond(Millisecond arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state, arg0.suffix);
    }
  }

  @Ek9Constructor("""
      Millisecond() as pure
        -> arg0 as String""")
  public Millisecond(String arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(Millisecond._of(arg0.state));
    }
  }

  @Ek9Method("""
      duration() as pure
        <- rtn as Duration?""")
  public Duration duration() {
    if (!isSet) {
      return new Duration();
    }
    BigDecimal bigDecimal = BigDecimal.valueOf(this.state);
    long seconds = bigDecimal.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_EVEN).longValue();

    return Duration._of(seconds);
  }

  public Duration _promote() {
    return duration();
  }

  @Ek9Operator("""
      operator mod as pure
        -> by as Millisecond
        <- rtn as Integer?""")
  public Integer _mod(Millisecond by) {

    if (canProcess(by)) {
      return Integer._of(state)._mod(Integer._of(by.state));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator rem as pure
        -> by as Millisecond
        <- rtn as Integer?""")
  public Integer _rem(Millisecond by) {
    if (canProcess(by)) {
      return Integer._of(state)._rem(Integer._of(by.state));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Millisecond""")
  public void _merge(Millisecond arg) {
    if (isValid(arg)) {
      if (isSet) {
        _addAss(arg);
      } else {
        assign(arg);
      }
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Millisecond""")
  public void _replace(Millisecond arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Millisecond""")
  public void _copy(Millisecond arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Millisecond""")
  public void _pipe(Millisecond arg) {
    _merge(arg);
  }

  //TODO more stock methods and operators like less than etc.

  @Ek9Operator("""
      operator - as pure
        <- rtn as Millisecond?""")
  public Millisecond _negate() {
    Millisecond rtn = _new();
    if (isSet) {
      rtn.assign(-this.state, this.suffix);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Integer?""")
  public Integer _prefix() {
    if (isSet) {
      return Integer._of(this.state);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as String?""")
  public String _suffix() {
    if (isSet) {
      return String._of(suffix);
    }
    return new String();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    return _string()._len();
  }

  @Ek9Method("""
      operator ++
        <- rtn as Millisecond?""")
  public Millisecond _inc() {
    if (isSet) {
      assign(state + 1, suffix);
    }
    return this;
  }

  @Ek9Method("""
      operator --
        <- rtn as Millisecond?""")
  public Millisecond _dec() {
    if (isSet) {
      assign(state - 1, suffix);
    }
    return this;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Millisecond
        <- rtn as Millisecond?""")
  public Millisecond _add(Millisecond arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._addAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Duration
        <- rtn as Millisecond?""")
  public Millisecond _add(Duration arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._addAss(arg);
    }
    return rtn;
  }


  @Ek9Operator("""
      operator - as pure
        -> arg as Millisecond
        <- rtn as Millisecond?""")
  public Millisecond _sub(Millisecond arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._subAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Duration
        <- rtn as Millisecond?""")
  public Millisecond _sub(Duration arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._subAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Millisecond?""")
  public Millisecond _mul(Float arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._mulAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Millisecond?""")
  public Millisecond _mul(Integer arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._mulAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Millisecond?""")
  public Millisecond _div(Float arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._divAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Millisecond?""")
  public Millisecond _div(Integer arg) {
    Millisecond rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._divAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Millisecond
        <- rtn as Float?""")
  public Float _div(Millisecond arg) {
    Float rtn = new Float();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(Integer._of(arg.state));
    }

    return rtn;
  }

  @Ek9Operator("""
      operator +=
        -> arg as Millisecond""")
  public void _addAss(Millisecond arg) {
    if (canProcess(arg)) {
      assign(this.state + arg.state, this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator +=
        -> arg as Duration""")
  public Millisecond _addAss(Duration arg) {
    if (canProcess(arg)) {
      assign(this.state + (arg._getAsSeconds() * 1000), this.suffix);
    } else {
      unSet();
    }
    return this;
  }

  @Ek9Operator("""
      operator -=
        -> arg as Millisecond""")
  public void _subAss(Millisecond arg) {
    if (canProcess(arg)) {
      assign(this.state - arg.state, this.suffix);
    } else {
      unSet();
    }
  }


  @Ek9Operator("""
      operator -=
        -> arg as Duration""")
  public void _subAss(Duration arg) {
    if (canProcess(arg)) {
      assign(this.state - (arg._getAsSeconds() * 1000), this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator *=
        -> arg as Integer""")
  public void _mulAss(Integer arg) {
    if (canProcess(arg)) {
      assign(this.state * arg.state, this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator *=
        -> arg as Float""")
  public void _mulAss(Float arg) {
    if (canProcess(arg)) {
      assign((long) (this.state * arg.state), this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator /=
        -> arg as Float""")
  public void _divAss(Float arg) {
    if (canProcess(arg) && !arg._empty().state) {
      assign((long) (this.state / arg.state), this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator /=
        -> arg as Integer""")
  public void _divAss(Integer arg) {
    if (canProcess(arg) && arg.state != 0) {
      assign((this.state / arg.state), this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Millisecond
        <- rtn as Integer?""")
  public Integer _cmp(Millisecond arg) {
    if (canProcess(arg)) {
      return Integer._of(this.compare(arg));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Millisecond asMillisecond) {
      return _cmp(asMillisecond);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Millisecond
        <- rtn as Boolean?""")
  public Boolean _lt(Millisecond arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Millisecond
        <- rtn as Boolean?""")
  public Boolean _lteq(Millisecond arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Millisecond
        <- rtn as Boolean?""")
  public Boolean _gt(Millisecond arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Millisecond
        <- rtn as Boolean?""")
  public Boolean _gteq(Millisecond arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) >= 0);
    }
    return new Boolean();
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof Millisecond asMillisecond) {
      return _eq(asMillisecond);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Millisecond
        <- rtn as Boolean?""")
  public Boolean _eq(Millisecond arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Millisecond
        <- rtn as Boolean?""")
  public Boolean _neq(Millisecond arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) != 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator abs as pure
        <- rtn as Millisecond?""")
  public Millisecond _abs() {
    Millisecond rtn = _new();
    if (this.isSet) {
      rtn.assign(Math.abs(state), this.suffix);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator sqrt as pure
        <- rtn as Float?""")
  public Float _sqrt() {
    if (isSet) {
      return Float._of(this.state)._sqrt();
    }
    return new Float();
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
      return String._of(state + suffix);
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
      rtn.assign(Objects.hashCode(state + suffix));
    }
    return rtn;
  }

  //Start of Utility methods

  private java.lang.Integer compare(Millisecond arg) {
    return Long.compare(this.state, arg.state);
  }

  private void assign(Millisecond arg) {
    if (isValid(arg)) {
      assign(arg.state, arg.suffix);
    } else {
      unSet();
    }
  }

  @SuppressWarnings("checkstyle:VariableDeclarationUsageDistance")
  private void assign(long theSize, java.lang.String theSuffix) {
    long stateBefore = this.state;
    java.lang.String suffixBefore = this.suffix;
    boolean beforeIsValid = isSet;

    state = theSize;
    suffix = theSuffix;
    set();
    if (!validateConstraints().isSet) {
      java.lang.String stringTo = this.toString();
      state = stateBefore;
      suffix = suffixBefore;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + this + " to " + stringTo);
    }
  }

  @Override
  public void unSet() {
    super.unSet();
    //and rest the amount
    if (suffix != null) {
      state = 0L;
    }
  }

  protected Millisecond _new() {
    return new Millisecond();
  }

  protected void parse(java.lang.String arg) {
    Pattern p = Pattern.compile("^(-?\\d+)(ms)$");
    Matcher m = p.matcher(arg);

    if (m.find()) {
      java.lang.String first = m.group(1);
      java.lang.String second = m.group(2);

      int theSize = java.lang.Integer.parseInt(first);
      assign(theSize, second);
    }
  }


  public static Millisecond _of(java.lang.String arg) {
    Millisecond rtn = new Millisecond();
    if (arg != null) {
      rtn.parse(arg);
    }

    return rtn;
  }

  public static Millisecond _of(long arg) {
    Millisecond rtn = new Millisecond();

    rtn.assign(arg, "ms");

    return rtn;
  }


}
