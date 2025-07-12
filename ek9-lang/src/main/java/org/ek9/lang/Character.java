package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * Represents a single Character - can be multi-byte (Unicode).
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Character as open""")
public class Character extends BuiltinType {

  char state = ' ';

  @Ek9Constructor("""
      Character() as pure""")
  public Character() {
    super.unSet();
  }

  @Ek9Constructor("""
      Character() as pure
        -> arg0 as Character""")
  public Character(Character arg0) {
    unSet();
    if (isValid(arg0)) {
      assign(arg0.state);
    }
  }

  @Ek9Constructor("""
      Character() as pure
        -> arg0 as String""")
  public Character(String arg0) {
    if (arg0 != null && arg0.isSet) {
      assign(Character._of(arg0.state));
    }
  }

  @Ek9Method("""
      upperCase() as pure
        <- rtn as Character?""")
  public Character upperCase() {
    Character rtn = _new();
    if (isSet) {
      rtn.assign(java.lang.Character.toUpperCase(this.state));
    }
    return rtn;
  }

  @Ek9Method("""
      lowerCase() as pure
        <- rtn as Character?""")
  public Character lowerCase() {
    Character rtn = _new();
    if (isSet) {
      rtn.assign(java.lang.Character.toLowerCase(this.state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Character
        <- rtn as Boolean?""")
  public Boolean _lt(Character arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state < arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <= as pure
        -> arg as Character
        <- rtn as Boolean?""")
  public Boolean _lteq(Character arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state <= arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Character
        <- rtn as Boolean?""")
  public Boolean _gt(Character arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state > arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator >= as pure
        -> arg as Character
        <- rtn as Boolean?""")
  public Boolean _gteq(Character arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state >= arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Character
        <- rtn as Boolean?""")
  public Boolean _eq(Character arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state == arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Character
        <- rtn as Boolean?""")
  public Boolean _neq(Character arg) {
    if (canProcess(arg)) {
      return Boolean._of(this.state != arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Character
        <- rtn as Integer?""")
  public Integer _cmp(Character arg) {
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

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Character asCharacter) {
      return _cmp(asCharacter);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Character
        <- rtn as Integer?""")
  public Integer _fuzzy(Character arg) {
    if (!this.canProcess(arg)) {
      return new Integer();
    }

    Levenshtein fuzzy = new Levenshtein();
    return Integer._of(fuzzy.costOfMatch(this._string().state, arg._string().state));
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(this.isSet);
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    if (isSet) {
      return String._of(this.state);
    }
    return new String();
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    return _promote();
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
      operator length as pure
        <- rtn as Integer?""")
  public Integer _len() {
    if (isSet) {
      return Integer._of(1);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator :~:
        -> arg as Character""")
  public void _merge(Character arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :^:
        -> arg as Character""")
  public void _replace(Character arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Character""")
  public void _copy(Character value) {
    if (isValid(value)) {
      assign(value);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Character""")
  public void _pipe(Character arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator ++
        <- rtn as Character?""")
  public Character _inc() {
    if (isSet) {
      final var possibleValidValue = state + 1;
      if (java.lang.Character.isDefined(possibleValidValue)) {
        assign((char) possibleValidValue);
      } else {
        unSet();
      }
    }
    return this;
  }

  @Ek9Operator("""
      operator --
        <- rtn as Character?""")
  public Character _dec() {
    if (isSet) {
      final var possibleValidValue = state - 1;
      if (java.lang.Character.isDefined(possibleValidValue)) {
        assign((char) possibleValidValue);
      } else {
        unSet();
      }
    }
    return this;
  }

  private void assign(Character value) {
    if (isValid(value)) {
      assign(value.state);
    }
  }

  private void assign(char value) {
    char before = state;
    boolean beforeIsValid = isSet;
    state = value;
    set();
    if (!validateConstraints().state) {
      state = before;
      isSet = beforeIsValid;
      throw new RuntimeException("Constraint violation can't change " + state + " to " + value);
    }

  }

  //Start of Utility methods

  /*
   * Why have a 'new method' so that extending classes can override and provide a new version of what they are.
   * Using covariance we can return a class that extends 'Integer' and use it as an Integer
   */
  protected Character _new() {
    return new Character();
  }

  @Override
  public int hashCode() {
    return java.lang.Character.hashCode(state);
  }

  @Override
  public boolean equals(Object obj) {
    if (super.equals(obj) && obj instanceof Character character) {
      if (isSet) {
        return state == (character.state);
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

  public static Character _of(Character value) {
    Character rtn = new Character();
    rtn.assign(value);
    return rtn;
  }

  public static Character _of(int value) {
    Character rtn = new Character();
    rtn.assign((char) value);
    return rtn;
  }

  public static Character _of(char value) {
    Character rtn = new Character();
    rtn.assign(value);
    return rtn;
  }

  public static Character _of(java.lang.String value) {
    Character rtn = new Character();
    if (value != null && !value.isEmpty()) {
      rtn.assign(value.charAt(0));
    }
    return rtn;
  }

}
