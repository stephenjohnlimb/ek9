package org.ek9.lang;

import java.util.UUID;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 GUID type that wraps Java's UUID.
 * The default constructor generates a new random UUID.
 * Invalid operations or null inputs may result in unset state, but constructors
 * always fallback to generating a new random UUID to ensure valid state.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("GUID")
public class GUID extends BuiltinType {

  UUID state;

  @Ek9Constructor("GUID() as pure")
  public GUID() {
    state = UUID.randomUUID();
    set(); // Always set, never unset
  }

  @Ek9Constructor("""
      GUID() as pure
        -> arg as GUID""")
  public GUID(GUID arg) {
    if (isValid(arg)) {
      state = arg.state;
    } else {
      state = UUID.randomUUID(); // Fallback to random UUID
    }
    set(); // Always set
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Constructor("""
      GUID() as pure
        -> arg as String""")
  public GUID(String arg) {
    if (isValid(arg)) {
      try {
        state = UUID.fromString(arg.state);
      } catch (IllegalArgumentException _) {
        state = UUID.randomUUID(); // Fallback to random UUID
      }
    } else {
      state = UUID.randomUUID(); // Fallback to random UUID
    }
    set(); // Always set
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as GUID
        <- rtn as Boolean?""")
  public Boolean _eq(GUID arg) {
    if (isValid(arg)) {
      return Boolean._of(this.state.equals(arg.state));
    }
    return new Boolean();
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as GUID
        <- rtn as Boolean?""")
  public Boolean _neq(GUID arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as GUID
        <- rtn as Integer?""")
  public Integer _cmp(GUID arg) {
    if (isValid(arg)) {
      int result = this.state.compareTo(arg.state);
      return Integer._of(result);
    }
    return new Integer(); // Return unset for invalid comparison
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof GUID asGUID) {
      return _cmp(asGUID);
    }
    return new Integer(); // Return unset for non-GUID or null comparison
  }

  @Ek9Operator("""
      operator :^:
        -> arg as GUID""")
  public void _replace(GUID arg) {
    _copy(arg); // Replace delegates to copy
  }

  @Ek9Operator("""
      operator :=:
        -> arg as GUID""")
  public void _copy(GUID value) {
    if (isValid(value)) {
      assign(value.state);
    } else {
      // For GUID, even invalid copy generates new UUID
      state = UUID.randomUUID();
      set();
    }
  }

  @Ek9Operator("""
      operator #^ as pure
        <- rtn as String?""")
  public String _promote() {
    return _string(); // Delegate to string operator
  }

  @Override
  @Ek9Operator("""
      operator $ as pure
        <- rtn as String?""")
  public String _string() {
    return String._of(state.toString()); // Always valid UUID string
  }

  @Override
  @Ek9Operator("""
      operator $$ as pure
        <- rtn as JSON?""")
  public JSON _json() {
    return new JSON(this._string());
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    return Integer._of(state.hashCode()); // Always valid
  }

  /**
   * Helper method to assign a new UUID value.
   *
   * @param value The UUID value to assign
   */
  public void assign(UUID value) {
    state = value;
    set(); // Always remain set
  }

  /**
   * Factory method to create GUID from Java string representation.
   *
   * @param uuidString Java string representation of UUID.
   * @return New GUID instance
   */
  public static GUID _of(java.lang.String uuidString) {
    return new GUID(String._of(uuidString));
  }

  /**
   * Factory method to create GUID from Java UUID.
   *
   * @param uuid Java UUID instance.
   * @return New GUID instance
   */
  public static GUID _of(UUID uuid) {
    GUID guid = new GUID();
    if (uuid != null) {
      guid.assign(uuid);
    }
    return guid;
  }

  /**
   * Factory method to generate a new random GUID.
   *
   * @return New GUID instance with random UUID.
   */
  public static GUID _of() {
    return new GUID();
  }
}