package org.ek9.lang;

import org.ek9tooling.Ek9Operator;

/**
 * Basically the super class/function of everything.
 * But not declared as a Construct or a Function.
 * This is 'baked' into the EK9 compiler.
 * <p>
 * But we still need to ensure that everything implement this so that
 * casting and returning of objects that are any sort of construct can be
 * dealt with as an Any.
 * </p>
 * <p>
 * The only thing is that 'Any' does have the _isSet operator.
 * </p>
 */
@SuppressWarnings("checkstyle:MethodName")
public interface Any {
  /**
   * While this interface does not get introspected, I've included the operator
   * annotation just for completeness.
   * Extending classes will need to implement this directly.
   *
   * @return Boolean true if the object has a meaningful value false otherwise.
   */
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  default Boolean _isSet() {
    return Boolean._of(false);
  }

  @Ek9Operator("""
      operator < as pure
        -> arg as Any
        <- rtn as Boolean?""")
  default Boolean _lt(Any arg) {
    final var cmpResult = this._cmp(arg);
    if (cmpResult.isSet) {
      return Boolean._of(cmpResult.state < 0);
    }

    return new Boolean();
  }


  @Ek9Operator("""
      operator <= as pure
        -> arg as Any
        <- rtn as Boolean?""")
  default Boolean _lteq(Any arg) {
    final var cmpResult = this._cmp(arg);
    if (cmpResult.isSet) {
      return Boolean._of(cmpResult.state <= 0);
    }

    return new Boolean();
  }

  @Ek9Operator("""
      operator > as pure
        -> arg as Any
        <- rtn as Boolean?""")
  default Boolean _gt(Any arg) {
    final var cmpResult = this._cmp(arg);
    if (cmpResult.isSet) {
      return Boolean._of(cmpResult.state > 0);
    }

    return new Boolean();
  }


  @Ek9Operator("""
      operator >= as pure
        -> arg as Any
        <- rtn as Boolean?""")
  default Boolean _gteq(Any arg) {
    final var cmpResult = this._cmp(arg);
    if (cmpResult.isSet) {
      return Boolean._of(cmpResult.state >= 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  default Boolean _eq(Any arg) {
    final var cmpResult = this._cmp(arg);
    if (cmpResult.isSet) {
      return Boolean._of(cmpResult.state == 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as Any
        <- rtn as Boolean?""")
  default Boolean _neq(Any arg) {
    final var cmpResult = this._cmp(arg);
    if (cmpResult.isSet) {
      return Boolean._of(cmpResult.state != 0);
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  default String _string() {
    return new String();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  default Integer _cmp(Any arg) {
    if (arg == null || !arg._isSet().isSet) {
      return new Integer();
    }
    if (this == arg) {
      return Integer._of(0);
    }
    //Expect classes to override.
    return new Integer();
  }

  /**
   * Now while you cannot 'new' an interface, you can do this to get an 'Any'.
   *
   * @return An instance of an 'Any'.
   */
  static Any _new() {
    return new Any() {
    };
  }

}
