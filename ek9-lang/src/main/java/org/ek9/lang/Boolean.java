
package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * Represents the Boolean type in ek9.
 * <p>
 * See <a href="https://www.ek9.io/builtInTypes.html#boolean">EK9 Boolean</a>.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class
public class Boolean extends BuiltinType {

  boolean state;

  @Ek9Constructor("""
      Boolean() as pure""")
  public Boolean() {
    super.unSet();
  }

  @Ek9Constructor("""
      Boolean() as pure
        -> arg0 as Boolean""")
  public Boolean(Boolean arg0) {
    assign(arg0);
  }

  @Ek9Constructor("""
      Boolean() as pure
        -> arg0 as String""")
  public Boolean(String arg0) {
    if (isValid(arg0)) {
      assign(java.lang.Boolean.parseBoolean(arg0.state));
    }
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Boolean
        <- rtn as Boolean?""")
  public Boolean _eq(Boolean arg) {
    if (canProcess(arg)) {
      return _of(this.state == arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Boolean
        <- rtn as Boolean?""")
  public Boolean _neq(Boolean arg) {
    if (canProcess(arg)) {
      return _of(this.state != arg.state);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Boolean
        <- rtn as Integer?""")
  public Integer _cmp(Boolean arg) {
    Integer rtn = new Integer();
    if (canProcess(arg)) {
      if (this.state && !arg.state) {
        rtn.assign(1);
      } else if (!this.state && arg.state) {
        rtn.assign(-1);
      } else {
        rtn.assign(0);
      }
    }
    return rtn;
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof Boolean asBoolean) {
      return _cmp(asBoolean);
    }
    return new Integer();
  }

  @Ek9Operator("""
      operator <~> as pure
        -> arg as Boolean
        <- rtn as Integer?""")
  public Integer _fuzzy(Boolean arg) {
    //For boolean fuzzy match is just compare.
    return _cmp(arg);
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  /**
   * This means 'not' or negate the value and supply a negated version of it.
   *
   * @return a negated version of the boolean.
   */
  @Ek9Operator("""
      operator ~ as pure
        <- rtn as Boolean?""")
  public Boolean _negate() {
    Boolean rtn = _new();
    if (isSet) {
      rtn.assign(!this.state);
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

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  @Override
  public String _string() {
    final var rtn = new String();
    if (isSet) {
      rtn.assign(this.state + "");
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
      rtn.assign(java.lang.Boolean.hashCode(state));
    }
    return rtn;
  }

  @Ek9Operator("""
      operator and as pure
        -> arg as Boolean
        <- rtn as Boolean?""")
  public Boolean _and(Boolean arg) {
    Boolean rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state && arg.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator or as pure
        -> arg as Boolean
        <- rtn as Boolean?""")
  public Boolean _or(Boolean arg) {
    Boolean rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state || arg.state);
    }
    return rtn;
  }

  @Ek9Operator("""
      operator xor as pure
        -> arg as Boolean
        <- rtn as Boolean?""")
  public Boolean _xor(Boolean arg) {
    Boolean rtn = _new();
    if (canProcess(arg)) {
      rtn.assign(this.state ^ arg.state);
    }
    return rtn;
  }

  /**
   * Seems a bit strange to use + to add two booleans
   * But can be useful.
   */
  @Ek9Operator("""
      operator + as pure
        -> arg as Boolean
        <- rtn as Boolean?""")
  public Boolean _add(Boolean arg) {
    return _or(arg);
  }

  /**
   * Useful for collecting up booleans to see if any one of them is true.
   * This does mutate the current arg.
   */
  @Ek9Operator("""
      operator +=
        -> arg as Boolean""")
  public void _addAss(Boolean arg) {
    //If presented with an unset value then this becomes unset.
    if (!isValid(arg)) {
      unSet();
    }
    if (isSet) {
      assign(_or(arg));
    }
  }

  /**
   * This is a mutation of this value.
   */
  @Ek9Operator("""
      operator :~:
        -> arg as Boolean""")
  public void _merge(Boolean arg) {
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
        -> arg as Boolean""")
  public void _replace(Boolean arg) {
    _copy(arg);
  }

  @Ek9Operator("""
      operator :=:
        -> arg as Boolean""")
  public void _copy(Boolean arg) {
    if (isValid(arg)) {
      assign(arg);
    } else {
      unSet();
    }
  }

  @Ek9Operator("""
      operator |
        -> arg as Boolean""")
  public void _pipe(Boolean arg) {
    _merge(arg);
  }

  @Ek9Operator("""
      operator |
        -> arg as JSON""")
  public void _pipe(JSON arg) {

    jsonTraversal.accept(arg, str -> _pipe(new Boolean(str)));
  }

  //Start of utility methods

  @Override
  protected Boolean _new() {
    return new Boolean();
  }

  //Note this is not declared as an EK9 operator.
  //Maybe this is just built in to the compiler as variable
  //assignment, rather than copying contents.
  void assign(Boolean arg) {
    if (arg.isSet) {
      assign(arg.state);
    }
  }

  void assign(boolean arg) {
    state = arg;
    //You cannot constrain a boolean! So unlike other types we don't try to constrain.
    set();
  }

  public static Boolean _of(java.lang.String arg) {
    return _of(java.lang.Boolean.parseBoolean(arg));
  }

  public static Boolean _of(boolean arg) {
    Boolean rtn = new Boolean();
    rtn.assign(arg);
    return rtn;
  }

}
