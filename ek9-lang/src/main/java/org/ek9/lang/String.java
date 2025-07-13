package org.ek9.lang;


import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * A mutable String, perhaps we also need an Immutable String, or perhaps we alter this string to be immutable
 * and create a mutable string.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    String as open""")
public class String extends BuiltinType implements Any {
  java.lang.String state = "";

  @Ek9Constructor("""
      String() as pure""")
  public String() {
    super.unSet();
  }

  @Ek9Constructor("""
      String() as pure
        -> arg0 as String""")
  public String(String arg0) {
    unSet();
    assign(arg0);
  }

  //TODO need Optional for Optional of String in a constructor.

  @Ek9Method("""
      trim() as pure
        <- rtn as String?""")
  public String trim() {
    if (isSet) {
      return String._of(this.state.trim());
    }
    return _new();
  }

  //TODO need Character for trimming by character.


  @Ek9Method("""
      upperCase() as pure
        <- rtn as String?""")
  public String upperCase() {
    String rtn = _new();
    if (isSet) {
      rtn.assign(String._of(this.state.toUpperCase()));
    }
    return rtn;
  }

  @Ek9Method("""
      lowerCase() as pure
        <- rtn as String?""")
  public String lowerCase() {
    String rtn = _new();
    if (isSet) {
      rtn.assign(String._of(this.state.toLowerCase()));
    }
    return rtn;
  }

  //TODO Iterator of Character

  //TODO first and last with Character

  @Ek9Method("""
      rightPadded() as pure
        -> totalWidth as Integer
        <- rtn as String?""")
  public String rightPadded(Integer totalWidth) {
    if (canProcess(totalWidth)) {
      final var formatStr = "%-" + totalWidth.state + "s";
      return String._of(java.lang.String.format(formatStr, this.state));
    }
    return _new();
  }

  @Ek9Method("""
      leftPadded() as pure
        -> totalWidth as Integer
        <- rtn as String?""")
  public String leftPadded(Integer totalWidth) {
    if (canProcess(totalWidth)) {
      final var formatStr = "%" + totalWidth.state + "s";
      return String._of(java.lang.String.format(formatStr, this.state));
    }
    return _new();
  }

  //TODO count with Character

  //TODO split with RegEX

  @Ek9Operator("""
      operator < as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _lt(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(compare(arg.state) < 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _lteq(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(compare(arg.state) <= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _gt(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(compare(arg.state) > 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _gteq(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(compare(arg.state) >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _eq(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _neq(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(!this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as String
        <- rtn as Integer?""")
  public Integer _cmp(String arg) {
    if (this.canProcess(arg)) {
      return Integer._of(compare(arg.state));
    }
    return new Integer();
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof String string) {
      return _cmp(string);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as String
        <- rtn as Integer?""")
  public Integer _fuzzy(String arg) {
    if (!this.canProcess(arg)) {
      return new Integer();
    }

    Levenshtein fuzzy = new Levenshtein();
    return Integer._of(fuzzy.costOfMatch(this.state, arg.state));
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator + as pure
        -> arg as String
        <- rtn as String?""")
  public String _add(String arg) {
    String rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(state + arg.state);
    }
    return rtn;
  }

  //TODO add Character

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    String rtn = new String();
    if (isSet) {
      rtn.assign(state);
    }
    return rtn;
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

  //TODO _prefix and _suffix returning a Character


  @Ek9Operator("""
      operator empty as pure
        <- rtn as Boolean?""")
  public Boolean _empty() {
    if (isSet) {
      return Boolean._of(this.state == null || this.state.isBlank());
    }

    return new Boolean();
  }

  @Ek9Operator("""
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (isSet) {
      return Integer._of(this.state.length());
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as String""")
  public void _merge(String arg) {
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
        -> arg as String""")
  public void _replace(String arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as String""")
  public void _copy(String value) {
    if (isValid(value)) {
      assign(value.state);
    } else {
      super.unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Any""")
  public void _pipe(Any arg) {
    _merge(arg._string());
  }

  @Ek9Operator("""
      operator +=
        -> arg as String""")
  public void _addAss(String value) {
    if (canProcess(value)) {
      assign(state + value.state);
    } else {
      unSet();
    }
  }

  //TODO addAss Character

  @Ek9Operator("""
      operator contains as pure
        -> arg as String
        <- rtn as Boolean?""")
  public Boolean _contains(String arg) {
    if (this.canProcess(arg)) {
      return Boolean._of(this.state.contains(arg.state));
    }
    return new Boolean();
  }

  //TODO matches with RegEx

  //Start of utility methods

  protected String _new() {
    return new String();
  }

  public void assign(String value) {
    if (isValid(value)) {
      assign(value.state);
    } else {
      super.unSet();
    }

  }

  public void assign(java.lang.String value) {
    java.lang.String before = state;
    boolean beforeIsValid = isSet;
    state = value;
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }

  }

  private int compare(java.lang.String to) {
    return this.state.compareTo(to);
  }


  public static String _of(char value) {
    String rtn = new String();
    rtn.assign(value + "");

    return rtn;
  }

  public static String _of(java.lang.String value) {
    String rtn = new String();
    if (value != null) {
      rtn.assign(value);
    }
    return rtn;
  }

  public static String _of(String arg) {
    String rtn = new String();
    if (isValid(arg)) {
      rtn.assign(arg);
    }
    return rtn;
  }

  public static String _of(Integer arg) {
    String rtn = new String();
    if (isValid(arg)) {
      rtn.assign(arg.toString());
    }
    return rtn;
  }

  public static String _of(Float arg) {
    String rtn = new String();
    if (isValid(arg)) {
      rtn.assign(arg.toString());
    }
    return rtn;
  }

}
