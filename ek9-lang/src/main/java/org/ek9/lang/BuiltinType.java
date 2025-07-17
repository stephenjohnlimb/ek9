package org.ek9.lang;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Built in type for ek9 constructs.
 * Has useful methods. All use '_' so there is no chance of any collisions with normal mthods.
 */
@SuppressWarnings("checkstyle:MethodName")
public abstract class BuiltinType implements Any {
  boolean isSet = false;

  /**
   * You must implement one of these so that we can create new instances of the specific type.
   *
   * @return use covariant to return the right type.
   */
  protected Any _new() {
    return new Any(){};
  }

  public void unSet() {
    this.isSet = false;
  }

  public void set() {
    this.isSet = true;
  }

  /**
   * Given the current build in type and the value built in type can the processing go ahead.
   *
   * @param value The value checked for null and isSet.
   * @return true if the processing can go ahead, false otherwise.
   */
  protected boolean canProcess(final BuiltinType value) {
    return isSet && isValid(value);
  }

  protected boolean nearEnoughToZero(final double arg) {
    return Math.abs(arg) < 10E-323;
  }

  /*
   * Special internal method to be called when state is altered
   * The state is set but an exception can result and the value set to its previous state.
   * This functionality is trigger in the _assign method - which is why it is critical it is always used.
   */
  protected Boolean validateConstraints() {
    //by default all is fine
    return Boolean._of(true);
  }

  protected boolean isBoundByDoubleQuotes(java.lang.String state) {
    Pattern p = Pattern.compile("^\"([^\"]*)\"$");
    Matcher m = p.matcher(state);
    return m.find();
  }

  public static boolean isValid(BuiltinType value) {
    return value != null && value.isSet;
  }

  @Override
  public boolean equals(final Object o) {

    //Cannot possibly be equal.
    if (!(o instanceof final Any any)) {
      return false;
    }

    //Might be equal, so delegate to the Ek9 implementation if defined to see.
    final var ek9Result = _eq(any);
    return ek9Result.isSet && ek9Result.state;

  }

  @Override
  public int hashCode() {

    //Delegate to the Ek9 implementation if defined beyond the default in 'Any'
    final var ek9Result = _hashcode();
    if (ek9Result.isSet) {
      return (int) ek9Result.state;
    }
    return java.lang.Boolean.hashCode(isSet);
  }

  @Override
  public java.lang.String toString() {

    //Also just delegate to Ek9 for a String representation.
    final var ek9Result = _string();
    if (ek9Result.isSet) {
      return ek9Result.state;
    }
    return "";
  }
}
