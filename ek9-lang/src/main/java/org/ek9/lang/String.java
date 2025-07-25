package org.ek9.lang;


import java.util.regex.Pattern;
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

  @Ek9Method("""
      trim() as pure
        <- rtn as String?""")
  public String trim() {
    if (isSet) {
      return String._of(this.state.trim());
    }
    return _new();
  }

  @Ek9Method("""
      trim() as pure
        -> char as Character
        <- rtn as String?""")
  public String trim(Character character) {
    if (!canProcess(character)) {
      return _new(); // Return unset String when canProcess fails
    }

    if (state.isEmpty()) {
      return String._of(state); // Return copy of empty string
    }

    // Escape the character for regex (handle special regex chars like ., *, +, etc.)
    java.lang.String charToTrim = Pattern.quote(java.lang.String.valueOf(character.state));

    // Regex: ^[char]+ removes from start, [char]+$ removes from end
    java.lang.String trimmed = state.replaceAll("^" + charToTrim + "+|" + charToTrim + "+$", "");

    return String._of(trimmed);
  }

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

  @Ek9Method("""
      iterator() as pure
        <- rtn as Iterator of Character?""")
  public _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408 iterator() {
    //This type is Iterator of Character.
    //So need each java char converted to an EK9 Character.
    if (isSet) {
      //First we need just an Iterator (Any) but actually it has to have Characters in it.
      final var iterator = Iterator._of(this.state.chars().mapToObj(Character::_of).map(Any.class::cast).iterator());
      return _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of(iterator);
    }
    return _Iterator_7E0CA90C1F947ECE11C43ED0BB21B854FFD82455CECBC0C3EA4CC4A6EA343408._of();

  }

  @Ek9Method("""
      first() as pure
        <- rtn as Character?""")
  public Character first() {
    if (isSet && !state.isEmpty()) {
      return Character._of(state.charAt(0));
    }
    return new Character(); // Return unset Character
  }

  @Ek9Method("""
      last() as pure
        <- rtn as Character?""")
  public Character last() {
    if (isSet && !state.isEmpty()) {
      return Character._of(state.charAt(state.length() - 1));
    }
    return new Character(); // Return unset Character
  }

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

  @Ek9Method("""
      count() as pure
        -> char as Character
        <- rtn as Integer?""")
  public Integer count(Character character) {
    if (!canProcess(character)) {
      return new Integer(); // Return unset Integer
    }

    if (state.isEmpty()) {
      return Integer._of(0); // Empty string has 0 occurrences
    }

    char targetChar = character.state;
    int count = 0;

    // Iterate through string counting occurrences
    for (int i = 0; i < state.length(); i++) {
      if (state.charAt(i) == targetChar) {
        count++;
      }
    }

    return Integer._of(count);
  }

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

  @Ek9Operator("""
      operator + as pure
        -> arg as Character
        <- rtn as String?""")
  public String _add(Character arg) {
    String rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(state + arg.state);
    }
    return rtn;
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

  @Ek9Operator("""
      operator #< as pure
        <- rtn as Character?""")
  public Character _prefix() {
    return first(); // Delegate to first() method
  }

  @Ek9Operator("""
      operator #> as pure
        <- rtn as Character?""")
  public Character _suffix() {
    return last(); // Delegate to last() method
  }

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
  public void _copy(String arg) {
    if (isValid(arg)) {
      assign(arg.state);
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
  public void _addAss(String arg) {
    if (canProcess(arg)) {
      assign(state + arg.state);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator +=
        -> arg as Character""")
  public void _addAss(Character arg) {
    if (canProcess(arg)) {
      assign(state + arg.state);
    } else {
      unSet();
    }
  }

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

  @Override
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


  public static String _of() {
    return new String();
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
