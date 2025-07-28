package org.ek9.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Definition of Resolution.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Resolution as open""")
public class Resolution extends SuffixedComponent {

  long state = 0;

  @Ek9Constructor("""
      Resolution() as pure""")
  public Resolution() {
    super.unSet();
  }

  @Ek9Constructor("""
      Resolution() as pure
        -> arg0 as Resolution""")
  public Resolution(Resolution arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state, arg0.suffix);
    }
  }

  @Ek9Constructor("""
      Resolution() as pure
        -> arg0 as String""")
  public Resolution(String arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(Resolution._of(arg0.state));
    }
  }

  @Ek9Operator("""
      operator mod as pure
        -> by as Resolution
        <- rtn as Integer?""")
  public Integer _mod(Resolution by) {

    if (canProcess(by)) {
      return Integer._of(state)._mod(Integer._of(by.state));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator rem as pure
        -> by as Resolution
        <- rtn as Integer?""")
  public Integer _rem(Resolution by) {
    if (canProcess(by)) {
      return Integer._of(state)._rem(Integer._of(by.state));
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Resolution""")
  public void _merge(Resolution arg) {
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
        -> arg as Resolution""")
  public void _replace(Resolution arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Resolution""")
  public void _copy(Resolution arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Resolution""")
  public void _pipe(Resolution arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator - as pure
        <- rtn as Resolution?""")
  public Resolution _negate() {
    Resolution rtn = _new();
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
        <- rtn as Resolution?""")
  public Resolution _inc() {
    if (isSet) {
      assign(state + 1, suffix);
    }
    return this;
  }

  @Ek9Method("""
      operator --
        <- rtn as Resolution?""")
  public Resolution _dec() {
    if (isSet) {
      assign(state - 1, suffix);
    }
    return this;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Resolution
        <- rtn as Resolution?""")
  public Resolution _add(Resolution arg) {
    Resolution rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._addAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Resolution
        <- rtn as Resolution?""")
  public Resolution _sub(Resolution arg) {
    Resolution rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._subAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Resolution?""")
  public Resolution _mul(Float arg) {
    Resolution rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._mulAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Resolution?""")
  public Resolution _mul(Integer arg) {
    Resolution rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._mulAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Resolution?""")
  public Resolution _div(Float arg) {
    Resolution rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._divAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Resolution?""")
  public Resolution _div(Integer arg) {
    Resolution rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state, this.suffix);
      rtn._divAss(arg);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator / as pure
        -> arg as Resolution
        <- rtn as Float?""")
  public Float _div(Resolution arg) {
    Float rtn = new Float();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(Integer._of(arg.state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator +=
        -> arg as Resolution""")
  public void _addAss(Resolution arg) {
    if (canProcess(arg)) {
      assign(this.state + arg.state, this.suffix);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator -=
        -> arg as Resolution""")
  public void _subAss(Resolution arg) {
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
      operator <~> as pure
        -> arg as Resolution
        <- rtn as Integer?""")
  public Integer _fuzzy(Resolution arg) {
    return _cmp(arg);
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Resolution
        <- rtn as Integer?""")
  public Integer _cmp(Resolution arg) {
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
    if (arg instanceof Resolution asResolution) {
      return _cmp(asResolution);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Resolution
        <- rtn as Boolean?""")
  public Boolean _lt(Resolution arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Resolution
        <- rtn as Boolean?""")
  public Boolean _lteq(Resolution arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Resolution
        <- rtn as Boolean?""")
  public Boolean _gt(Resolution arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Resolution
        <- rtn as Boolean?""")
  public Boolean _gteq(Resolution arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Resolution
        <- rtn as Boolean?""")
  public Boolean _eq(Resolution arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Resolution
        <- rtn as Boolean?""")
  public Boolean _neq(Resolution arg) {
    if (canProcess(arg)) {
      return Boolean._of(compare(arg) != 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator abs as pure
        <- rtn as Resolution?""")
  public Resolution _abs() {
    Resolution rtn = _new();
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
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    if (isSet) {
      return new JSON(this);
    }
    return new JSON();
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
      rtn.assign(31L * Long.hashCode(state) + suffix.hashCode());
    }
    return rtn;
  }


  //Start of Utility methods

  private boolean canProcess(final Resolution resolution) {
    return super.canProcess(resolution) && this.suffix.equals(resolution.suffix);
  }

  private java.lang.Integer compare(Resolution arg) {

    return Long.compare(this.state, arg.state);

  }

  private void assign(Resolution arg) {
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

    if (!theSuffix.equals("dpi") && !theSuffix.equals("dpc")) {
      unSet();
      return;
    }

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

  @Override
  protected Resolution _new() {
    return new Resolution();
  }

  protected void parse(java.lang.String arg) {
    Pattern p = Pattern.compile("^(-?\\d+)(dp[i|c])$");
    Matcher m = p.matcher(arg);

    if (m.find()) {
      java.lang.String first = m.group(1);
      java.lang.String second = m.group(2);

      int theSize = java.lang.Integer.parseInt(first);
      assign(theSize, second);
    }
  }

  public static Resolution _of(java.lang.String arg) {
    Resolution rtn = new Resolution();
    if (arg != null) {
      rtn.parse(arg);
    }
    return rtn;
  }
}
