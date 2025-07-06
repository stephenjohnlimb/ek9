package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * Ek9 Integers are really longs.
 * Still trying to find the right model of developing components - now need to ensure that they can be extended via
 * 'defines type' but with constraints.
 * So we have to pay attention to creation and construction,
 * so that the right methods can be overridden and the new type returned.
 * <p>
 * Any alteration to state must go through the _assign method
 * - this so that we can control the call to check constraints.
 * </p>
 * Note that for literals you can still use the static constructors.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Integer as open""")
public class Integer extends BuiltinType {
  long state = 0;

  @Ek9Constructor("Integer() as pure")
  public Integer() {
    //Integer of what? so not set yet.
    super.unSet();
  }

  @Ek9Constructor("""
      Integer() as pure
        -> arg0 as Integer""")
  public Integer(Integer arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state);
    }
  }

  @Ek9Constructor("""
      Integer() as pure
        -> arg0 as String""")
  public Integer(String arg0) {
    unSet();
    if (isValid(arg0)) {
      final var possibleInteger = parse(arg0.state);
      if (possibleInteger != null) {
        assign(possibleInteger);
      }
    }
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Integer
        <- rtn as Boolean?""")
  public Boolean _lt(Integer arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state < arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator <= as pure
        -> arg as Integer
        <- rtn as Boolean?""")
  public Boolean _lteq(Integer arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state <= arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator > as pure
        -> arg as Integer
        <- rtn as Boolean?""")
  public Boolean _gt(Integer arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state > arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator >= as pure
        -> arg as Integer
        <- rtn as Boolean?""")
  public Boolean _gteq(Integer arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state >= arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator == as pure
        -> arg as Integer
        <- rtn as Boolean?""")
  public Boolean _eq(Integer arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state == arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator <> as pure
        -> arg as Integer
        <- rtn as Boolean?""")
  public Boolean _neq(Integer arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state != arg.state);
    }
    return new Boolean();
  }


  @Ek9Operator("""
      operator <=> as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _cmp(Integer arg) {
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
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _fuzzy(Integer arg) {
    return _cmp(arg);
  }

  @Ek9Operator("""
      operator sqrt as pure
        <- rtn as Float?""")
  public Float _sqrt() {
    if (isSet && this.state > 0) {
      return Float._of(Math.sqrt(state));
    }
    return new Float();
  }

  @Ek9Operator("""
      operator ! as pure
        <- rtn as Integer?""")
  public Integer _fac() {
    Integer rtn = _new();
    if (isSet && this.state >= 0) {
      rtn.assign(factorial(this.state));
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
        <- rtn as Integer?""")
  public Integer _negate() {
    Integer rtn = _new();
    if (isSet) {
      rtn.assign(-this.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _add(Integer arg) {
    Integer rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(state + arg.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _add(Float arg) {
    Float rtn = arg._new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._addAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _sub(Integer arg) {
    Integer rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._subAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator - as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _sub(Float arg) {
    Float rtn = arg._new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._subAss(arg);
    }

    return rtn;
  }


  @Ek9Operator("""
      operator * as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _mul(Integer arg) {
    Integer rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._mulAss(arg);
    }

    return rtn;
  }


  @Ek9Operator("""
      operator * as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _mul(Float arg) {
    Float rtn = arg._new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._mulAss(arg);
    }

    return rtn;
  }


  @Ek9Operator("""
      operator / as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _div(Integer arg) {
    Integer rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(arg);
    }

    return rtn;
  }


  @Ek9Operator("""
      operator / as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _div(Float arg) {
    Float rtn = arg._new();
    if (canProcess(arg)) {
      rtn.assign(this.state);
      rtn._divAss(arg);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator ^ as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Float _pow(Integer arg) {
    if (canProcess(arg)) {
      return Float._of(Math.pow(this.state, arg.state));
    }
    return new Float();
  }


  @Ek9Operator("""
      operator ^ as pure
        -> arg as Float
        <- rtn as Float?""")
  public Float _pow(Float arg) {
    Float rtn = new Float();
    if (canProcess(arg)) {
      rtn.assign(Math.pow(this.state, arg.state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as Float?""")
  public Float _promote() {
    if (isSet) {
      return Float._of(this);
    }
    return new Float();
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
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
      final var asStr = java.lang.Long.toString(state);
      if (asStr.startsWith("-")) {
        rtn.assign(java.lang.Long.parseLong(asStr.substring(0, 2)));
      } else {
        //Just get the first character and convert to long.
        rtn.assign(java.lang.Long.parseLong(asStr.substring(0, 1)));
      }
    }
    return rtn;
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as Integer?""")
  public Integer _suffix() {
    final var rtn = new Integer();
    if (isSet) {
      final var asStr = java.lang.Long.toString(state);
      final var length = asStr.length();
      final var val = java.lang.Long.parseLong(asStr.substring(length - 1, length));
      if (asStr.startsWith("-")) {
        rtn.assign(-1 * val);
      } else {
        rtn.assign(val);
      }
    }
    return rtn;
  }

  @Ek9Operator("""
      operator abs as pure
        <- rtn as Integer?""")
  public Integer _abs() {
    Integer rtn = _new();
    if (isSet) {
      rtn.assign(Math.abs(state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    //Means it is zero
    Boolean rtn = new Boolean();
    if (isSet) {
      rtn.assign(state == 0);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    Integer rtn = new Integer();
    if (isSet) {
      rtn.assign(java.lang.Long.toString(state).length());
    }
    return rtn;
  }

  @Ek9Operator("""
      operator and as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _and(Integer arg) {
    Integer rtn = new Integer();
    if (canProcess(arg)) {
      rtn.assign(state & arg.state);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator or as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _or(Integer arg) {
    Integer rtn = new Integer();
    if (canProcess(arg)) {
      rtn.assign(state | arg.state);
    }

    return rtn;
  }

  @Ek9Operator("""
      operator xor as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _xor(Integer arg) {
    Integer rtn = new Integer();
    if (canProcess(arg)) {
      rtn.assign(this.state ^ arg.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator mod as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _mod(Integer by) {
    //This is the real modulus operator "(a % b + b) % b"
    Integer rtn = _new();
    if (isSet && isValid(by)) {
      rtn.assign((state % by.state + by.state) % by.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator rem as pure
        -> arg as Integer
        <- rtn as Integer?""")
  public Integer _rem(Integer by) {
    //Yes this is really the remainder operator
    Integer rtn = _new();
    if (isSet && isValid(by)) {
      rtn.assign(state % by.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Integer""")
  public void _merge(Integer arg) {
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
        -> arg as Integer""")
  public void _replace(Integer arg) {
    _copy(arg);
  }

  /**
   * Handy when you want to set the arg inside a function, normal := just alters the object you point to
   * This, sets the underlying data so used more like a holder i.e. copy/clone.
   *
   * @param arg The arg to set this to.
   */
  @Ek9Operator("""
      operator :=:
        -> arg as Integer""")
  public void _copy(Integer arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Integer""")
  public void _pipe(Integer arg) {
    _merge(arg);
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
        -> arg as Integer""")
  public void _divAss(Integer arg) {

    if (canProcess(arg) && arg.state != 0) {
      assign(state / arg.state);
      return;
    }

    unSet();

  }

  @Ek9Operator("""
      operator ++
        <- rtn as Integer?""")
  public Integer _inc() {
    if (isSet) {
      assign(state + 1);
    }
    return this;
  }

  @Ek9Operator("""
      operator --
        <- rtn as Integer?""")
  public Integer _dec() {
    if (isSet) {
      assign(state - 1);
    }
    return this;
  }

  //Start of utility methods

  @Override
  protected Integer _new() {
    return new Integer();
  }

  private void assign(Integer arg) {
    if (arg.isSet) {
      assign(arg.state);
    }
  }

  public void assign(long arg) {
    long before = state;
    boolean beforeIsValid = isSet;
    state = arg;
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + arg);
    }

  }

  @Override
  public int hashCode() {
    return Long.hashCode(state);
  }


  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof Integer value) {
      if (isSet) {
        return state == value.state;
      }
      return true;
    }
    return false;
  }


  @Override
  public java.lang.String toString() {
    if (isSet) {
      return this.state + "";
    }
    return "";
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  private static Long parse(java.lang.String arg) {
    if (arg != null) {
      try {
        return Long.parseLong(arg);
      } catch (NumberFormatException _) {
        //just respond with null
      }
    }
    return null;
  }

  private static long factorial(long n) {
    if (n == 0) {
      return 1;
    } else {
      return (n * factorial(n - 1));
    }
  }

  public static Integer _of(java.lang.String arg) {
    final var rtn = new Integer();

    final var possibleLong = parse(arg);
    if (possibleLong != null) {
      rtn.assign(possibleLong);
    }
    return rtn;
  }

  public static Integer _of(Integer arg) {
    Integer rtn = new Integer();
    if (arg != null) {
      rtn.assign(arg);
    }
    return rtn;
  }

  public static Integer _of(int arg) {
    Integer rtn = new Integer();
    rtn.assign(arg);
    return rtn;
  }

  public static Integer _of(long arg) {
    Integer rtn = new Integer();
    rtn.assign(arg);
    return rtn;
  }


}