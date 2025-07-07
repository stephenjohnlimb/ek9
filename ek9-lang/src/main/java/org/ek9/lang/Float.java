package org.ek9.lang;


import java.math.BigDecimal;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 representation of a Float.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Float as open""")
public class Float extends BuiltinType {
  double state = 0;

  @Ek9Constructor("""
      Float() as pure""")
  public Float() {
    super.unSet();
  }

  @Ek9Constructor("""
      Float() as pure
        -> arg0 as Float""")
  public Float(Float arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state);
    }
  }

  @Ek9Constructor("""
      Float() as pure
        -> arg0 as String""")
  public Float(String arg0) {
    unSet();
    if (isValid(arg0)) {
      Double asDouble = Float.parse(arg0.state);
      if (asDouble != null) {
        assign(asDouble);
      }
    }
  }

  @Ek9Constructor("""
      Float() as pure
        -> arg0 as Integer""")
  public Float(Integer arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state);
    }
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Float
        <- rtn as Boolean?""")
  public Boolean _lt(Float arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state < arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Float
        <- rtn as Boolean?""")
  public Boolean _lteq(Float arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state <= arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator > as pure
        -> arg as Float
        <- rtn as Boolean?""")
  public Boolean _gt(Float arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state > arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Float
        <- rtn as Boolean?""")
  public Boolean _gteq(Float arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state >= arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Float
        <- rtn as Boolean?""")
  public Boolean _eq(Float arg) {
    if (canProcess(arg)) {
      return Boolean._of(nearEnoughToZero(this.state - arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Float
        <- rtn as Boolean?""")
  public Boolean _neq(Float arg) {
    if (canProcess(arg)) {
      return Boolean._of(!nearEnoughToZero(this.state - arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Float
        <- rtn as Integer?""")
  public Integer _cmp(Float arg) {
    if (canProcess(arg)) {
      if (this.state < arg.state) {
        return Integer._of(-1);
      }
      if (this.state > arg.state) {
        return Integer._of(1);
      }
      return Integer._of(0);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Float
        <- rtn as Integer?""")
  public Integer _fuzzy(Float arg) {
    return _cmp(arg);
  }

  @Ek9Operator("""
      operator sqrt as pure
        <- rtn as Float?""")
  public Float _sqrt() {
    Float rtn = _new();
    if (this.isSet && this.state > 0) {
      rtn.assign(Math.sqrt(state));
    }
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator - as pure
        <- rtn as Float?""")
  public Float _negate() {
    Float rtn = _new();
    if (this.isSet) {
      rtn.assign(-this.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _add(Float arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._addAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Integer
        <- rtn as Float?""")
  public Float _add(Integer arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._addAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _sub(Float arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._subAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Integer
        <- rtn as Float?""")
  public Float _sub(Integer arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
    }
    rtn._subAss(arg);
    return rtn;
  }


  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _mul(Float arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._mulAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Float?""")
  public Float _mul(Integer arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._mulAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _div(Float arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Float?""")
  public Float _div(Integer arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(arg);
    }

    return rtn;
  }


  @Ek9Operator("""
      operator ^ as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _pow(Float arg) {
    Float rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(Math.pow(this.state, arg.state));
    }
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    if (isSet) {
      return String._of(this.state + "");
    }
    return new String();
  }

  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    final var rtn = new Integer();
    if (isSet) {
      rtn.assign(this.hashCode());
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Integer?""")
  public Integer _prefix() {
    final var rtn = new Integer();
    if (isSet) {
      final var longPart = new BigDecimal(java.lang.String.valueOf(this.state)).longValue();
      rtn.assign(longPart);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as Float?""")
  public Float _suffix() {
    final var rtn = new Float();
    if (isSet) {
      final var fullAmount = new BigDecimal(java.lang.String.valueOf(this.state));
      final var longPart = fullAmount.longValue();
      final var decimalPart = fullAmount.subtract(new BigDecimal(longPart));
      rtn.assign(decimalPart.doubleValue());
    }
    return rtn;
  }

  @Ek9Operator("""
      operator abs as pure
        <- rtn as Float?""")
  public Float _abs() {
    Float rtn = _new();
    if (this.isSet) {
      rtn.assign(Math.abs(state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    //This is what is deemed to be empty, i.e. as close to zero as possible where an arg is present.
    Boolean rtn = new Boolean();
    if (isSet) {
      rtn.assign(nearEnoughToZero(state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    Integer rtn = new Integer();
    if (isSet) {
      rtn.assign(java.lang.Double.toString(state).length());
    }
    return rtn;
  }

  /**
   * Merge operator.
   * If both args are set then just addition.
   * If the current is unset - then copy.
   */
  @Ek9Operator("""
      operator :~:
        -> arg as Float""")
  public void _merge(Float arg) {

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
        -> arg as Float""")
  public void _replace(Float arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Float""")
  public void _copy(Float arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Float""")
  public void _pipe(Float arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator +=
        -> arg as Float""")
  public void _addAss(Float arg) {
    if (canProcess(arg)) {
      assign(state + arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Integer""")
  public void _addAss(Integer arg) {
    if (canProcess(arg)) {
      assign(state + arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as Float""")
  public void _subAss(Float arg) {
    if (canProcess(arg)) {
      assign(state - arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as Integer""")
  public void _subAss(Integer arg) {
    if (canProcess(arg)) {
      assign(state - arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator *=
        -> arg as Float""")
  public void _mulAss(Float arg) {
    if (canProcess(arg)) {
      assign(state * arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator *=
        -> arg as Integer""")
  public void _mulAss(Integer arg) {
    if (canProcess(arg)) {
      assign(state * arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator /=
        -> arg as Float""")
  public void _divAss(Float arg) {
    if (canProcess(arg)) {
      final var nearZero = arg._empty();
      if (nearZero.state || !nearZero.isSet) {
        unSet();
      } else {
        assign(state / arg.state);
      }
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator /=
        -> arg as Integer""")
  public void _divAss(Integer arg) {
    if (canProcess(arg)) {
      if (arg.state == 0) {
        unSet();
      } else {
        assign(state / arg.state);
      }
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator ++
        <- rtn as Float?""")
  public Float _inc() {
    if (this.isSet) {
      assign(state + 1);
    }
    return this;
  }

  @Ek9Operator("""
      operator --
        <- rtn as Float?""")
  public Float _dec() {
    if (this.isSet) {
      assign(state - 1);
    }
    return this;
  }

  //Start of utility methods.

  @Override
  protected Float _new() {
    return new Float();
  }

  private void assign(Float arg) {
    if (isValid(arg)) {
      assign(arg.state);
    }
  }

  public void assign(double arg) {

    if (!Double.isFinite(arg)) {
      unSet();
      return;
    }

    double before = state;
    boolean beforeIsValid = isSet;
    state = arg;
    set();
    if (!validateConstraints().isSet) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + arg);
    }

  }

  @Override
  public int hashCode() {
    return Double.hashCode(state);
  }

  /**
   * Compares within a threshold.
   * For full math checking programmer should be checking within their own threshold limits and not using equals.
   */
  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof Float value) {
      if (isSet) {
        return nearEnoughToZero(state - value.state);
      }
      return true;
    }
    return false;
  }

  @Override
  public java.lang.String toString() {
    if (this.isSet) {
      return this.state + "";
    }
    return "";
  }

  private boolean nearEnoughToZero(final double arg) {
    return Math.abs(arg) < 10E-323;
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  public static Double parse(java.lang.String arg) {
    if (arg != null) {
      try {
        return Double.parseDouble(arg);
      } catch (NumberFormatException _) {
        //just respond with null
      }
    }
    return null;
  }

  public static Float _of(Float arg) {
    return new Float(arg);
  }

  public static Float _of(java.lang.String arg) {
    Float rtn = new Float();
    final var possibleValue = parse(arg);
    if (possibleValue != null) {
      rtn.assign(possibleValue);
    }
    return rtn;
  }

  public static Float _of(float arg) {
    Float rtn = new Float();
    rtn.assign(arg);
    return rtn;
  }

  public static Float _of(Double arg) {
    Float rtn = new Float();
    if (arg != null) {
      rtn.assign(arg);
    }
    return rtn;
  }

  public static Float _of(Integer arg) {
    Float rtn = new Float();
    if (isValid(arg)) {
      rtn.assign(arg.state);
    }
    return rtn;
  }

  public static Float _of(long arg) {
    Float rtn = new Float();
    rtn.assign(arg);
    return rtn;
  }
}