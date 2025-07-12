package org.ek9.lang;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Definition of Dimension.
 * For strongly typed lengths etc.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Dimension as open""")
public class Dimension extends SuffixedComponent {

  //Take the same restrictions and definitions for the ek9 grammar for checking String convertions.
  private static final List<java.lang.String> validSuffix = List.of(
      "km", "m", "cm", "mm", "mile", "in", "pc", "pt", "px", "em", "ex", "ch", "rem", "vw", "vh", "vmin", "vmax", "%");

  double state = 0;

  @Ek9Constructor("""
      Dimension() as pure""")
  public Dimension() {
    super.unSet();
  }

  @Ek9Constructor("""
      Dimension() as pure
        -> arg0 as Dimension""")
  public Dimension(Dimension arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state, arg0.suffix);
    }
  }

  @Ek9Constructor("""
      Dimension() as pure
        -> arg0 as String""")
  public Dimension(String arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(Dimension._of(arg0.state));
    }
  }

  public Dimension(Float value, String suffix) {
    unSet();
    if (isValid(value) && isValid(suffix)) {
      assign(value.state, suffix.state);
    }
  }


  @Ek9Operator("""
      operator :~:
        -> arg as Dimension""")
  public void _merge(Dimension arg) {
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
        -> arg as Dimension""")
  public void _replace(Dimension arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Dimension""")
  public void _copy(Dimension arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Dimension""")
  public void _pipe(Dimension arg) {
    _merge(arg);
  }

  //TODO more stock methods and operators like less than etc.

  @Ek9Operator("""
      operator - as pure
        <- rtn as Dimension?""")
  public Dimension _negate() {
    Dimension rtn = _new();
    if (isSet) {
      rtn.assign(-this.state, this.suffix);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Float?""")
  public Float _prefix() {
    if (isSet) {
      return Float._of(this.state);
    }
    return new Float();
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

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    return _string()._len();
  }

  @Ek9Method("""
      operator ++
        <- rtn as Dimension?""")
  public Dimension _inc() {
    if (isSet) {
      assign(state + 1, suffix);
    }
    return this;
  }

  @Ek9Method("""
      operator --
        <- rtn as Dimension?""")
  public Dimension _dec() {
    if (isSet) {
      assign(state - 1, suffix);
    }
    return this;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Dimension
        <- rtn as Dimension?""")
  public Dimension _add(Dimension arg) {
    Dimension rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._addAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Dimension
        <- rtn as Dimension?""")
  public Dimension _sub(Dimension arg) {
    Dimension rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._subAss(arg);
    }
    return rtn;
  }


  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Dimension?""")
  public Dimension _mul(Float arg) {
    Dimension rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._mulAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Dimension?""")
  public Dimension _mul(Integer arg) {
    Dimension rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._mulAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Dimension?""")
  public Dimension _div(Float arg) {
    Dimension rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._divAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Dimension?""")
  public Dimension _div(Integer arg) {
    Dimension rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._divAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Dimension
        <- rtn as Float?""")
  public Float _div(Dimension arg) {
    Float rtn = new Float();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(Float._of(arg.state));
    }

    return rtn;
  }

  @Ek9Operator("""
      operator +=
        -> arg as Dimension""")
  public void _addAss(Dimension arg) {
    if (canProcess(arg)) {
      assign(this.state + arg.state, this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator -=
        -> arg as Dimension""")
  public void _subAss(Dimension arg) {
    if (canProcess(arg)) {
      assign(this.state - arg.state, this.suffix);
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
      assign(this.state / arg.state, this.suffix);
    } else {
      unSet();
    }

  }

  @Ek9Operator("""
      operator /=
        -> arg as Integer""")
  public void _divAss(Integer arg) {
    if (canProcess(arg) && arg.state != 0) {
      assign(this.state / arg.state, this.suffix);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Dimension
        <- rtn as Integer?""")
  public Integer _cmp(Dimension arg) {
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
    if (arg instanceof Dimension asDimension) {
      return _cmp(asDimension);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Dimension
        <- rtn as Boolean?""")
  public Boolean _lt(Dimension arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Dimension
        <- rtn as Boolean?""")
  public Boolean _lteq(Dimension arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Dimension
        <- rtn as Boolean?""")
  public Boolean _gt(Dimension arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Dimension
        <- rtn as Boolean?""")
  public Boolean _gteq(Dimension arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Dimension
        <- rtn as Boolean?""")
  public Boolean _eq(Dimension arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Dimension
        <- rtn as Boolean?""")
  public Boolean _neq(Dimension arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) != 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator abs as pure
        <- rtn as Dimension?""")
  public Dimension _abs() {
    Dimension rtn = _new();
    if (this.isSet) {
      rtn.assign(Math.abs(state), this.suffix);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator sqrt as pure
        <- rtn as Dimension?""")
  public Dimension _sqrt() {
    Dimension rtn = _new();
    if (isSet && this.state > 0.0) {
      rtn.assign(Math.sqrt(state), this.suffix);

    }
    return rtn;
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    if (isSet) {
      return String._of(this.toString());
    }
    return new String();
  }

  //Start of Utility methods

  private boolean canProcess(final Dimension dimension) {
    return super.canProcess(dimension) && this.suffix.equals(dimension.suffix);
  }

  private java.lang.Integer compare(Dimension arg) {
    return Double.compare(this.state, arg.state);
  }

  private void assign(Dimension arg) {
    if (isValid(arg)) {
      assign(arg.state, arg.suffix);
    } else {
      unSet();
    }
  }

  private void assign(double theSize, java.lang.String theSuffix) {

    if (!Double.isFinite(theSize)) {
      unSet();
      return;
    }

    if (!validSuffix.contains(theSuffix)) {
      unSet();
      return;
    }

    double stateBefore = this.state;

    boolean beforeIsValid = isSet;
    java.lang.String suffixBefore = this.suffix;

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
    //and reset the amount
    if (suffix != null) {
      state = 0L;
    }
  }

  @Override
  public int hashCode() {
    return Double.hashCode(state) + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof Dimension dimension) {
      if (isSet) {
        return state == dimension.state && suffix.equals(dimension.suffix);
      }
      return true;
    }
    return false;
  }

  @Override
  public java.lang.String toString() {
    if (isSet) {
      return state + suffix;
    }
    return "";
  }


  protected Dimension _new() {
    return new Dimension();
  }

  protected void parse(java.lang.String value) {
    Pattern p = Pattern.compile("^(-?\\d+)(\\.\\d+)?([a-z%]+)$");
    Matcher m = p.matcher(value);

    if (m.find()) {
      java.lang.String first = m.group(1);
      java.lang.String second = m.group(2);
      java.lang.String third = m.group(3);

      if (second != null) {
        //there is a floating point part.
        double theSize = java.lang.Double.parseDouble(first + second);
        assign(theSize, third);
      } else {
        //It is just an integer definition.
        double theSize = java.lang.Double.parseDouble(first);
        assign(theSize, third);
      }
    }
  }

  public static Dimension _of(java.lang.String arg) {
    Dimension rtn = new Dimension();
    if (arg != null) {
      rtn.parse(arg);
    }

    return rtn;
  }

}
