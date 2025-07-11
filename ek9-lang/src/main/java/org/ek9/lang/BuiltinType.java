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
  protected abstract BuiltinType _new();

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
   * @return true if the processing can go ahead, false otherised.
   */
  protected boolean canProcess(final BuiltinType value) {
    return isSet && isValid(value);
  }

  protected boolean canProcess(final Any value) {
    return isSet && isValid(value);
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

  public static boolean isValid(Any value) {
    if (value != null) {
      final var set = value._isSet();
      return set.isSet && set.state;
    }
    return false;
  }

  @Override
  public boolean equals(final Object o) {
    if (!(o instanceof final BuiltinType that)) {
      return false;
    }

    return isSet == that.isSet;
  }

  @Override
  public int hashCode() {
    return java.lang.Boolean.hashCode(isSet);
  }
}
