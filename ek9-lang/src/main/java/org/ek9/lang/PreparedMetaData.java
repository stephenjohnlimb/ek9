package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 PreparedMetaData type that contains metadata about a prepared execution point.
 * <p>
 * PreparedMetaData is set only when the contained JoinPoint is set and valid.
 * This makes it useful for aspect-oriented programming and execution metadata contexts.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * metaData &lt;- PreparedMetaData(JoinPoint("MyClass", "myMethod"))
 * if metaData?
 *   Stdout().println(metaData)
 * </pre>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("PreparedMetaData as open")
public class PreparedMetaData extends BuiltinType {

  private JoinPoint joinPoint = new JoinPoint();

  @Ek9Constructor("PreparedMetaData() as pure")
  public PreparedMetaData() {
    // Default constructor creates unset PreparedMetaData
    unSet();
  }

  @Ek9Constructor("""
      PreparedMetaData() as pure
        -> joinPoint as JoinPoint""")
  public PreparedMetaData(JoinPoint joinPoint) {
    if (isValid(joinPoint)) {
      this.joinPoint = joinPoint;
    }
    updateSetState();
  }

  @Ek9Method("""
      joinPoint() as pure
        <- rtn as JoinPoint?""")
  public JoinPoint joinPoint() {
    return joinPoint;
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof PreparedMetaData asPreparedMetaData) {
      return _eq(asPreparedMetaData);
    }
    return new Boolean(); // Return unset for non-PreparedMetaData comparison
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as PreparedMetaData
        <- rtn as Boolean?""")
  public Boolean _eq(PreparedMetaData arg) {
    if (!canProcess(arg)) {
      return new Boolean(); // Return unset for invalid comparison
    }

    // Delegate to JoinPoint comparison
    return this.joinPoint._eq(arg.joinPoint);
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as PreparedMetaData
        <- rtn as Boolean?""")
  public Boolean _neq(PreparedMetaData arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as PreparedMetaData
        <- rtn as Integer?""")
  public Integer _cmp(PreparedMetaData arg) {
    if (!canProcess(arg)) {
      return new Integer(); // Return unset for invalid comparison
    }

    // Delegate to JoinPoint comparison
    return this.joinPoint._cmp(arg.joinPoint);
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof PreparedMetaData asPreparedMetaData) {
      return _cmp(asPreparedMetaData);
    }
    return new Integer(); // Return unset for non-PreparedMetaData comparison
  }

  @Ek9Operator("""
      operator :~:
        -> arg as PreparedMetaData""")
  public void _merge(PreparedMetaData arg) {
    if (!isSet && isValid(arg)) {
      _copy(arg);
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as PreparedMetaData""")
  public void _replace(PreparedMetaData arg) {
    _copy(arg); // Replace delegates to copy
  }

  @Ek9Operator("""
      operator :=:
        -> arg as PreparedMetaData""")
  public void _copy(PreparedMetaData arg) {
    if (isValid(arg)) {
      this.joinPoint._copy(arg.joinPoint);
      updateSetState();
    } else {
      this.joinPoint = new JoinPoint();
      unSet();
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
    if (!isSet) {
      return String._of("PreparedMetaData{}");
    }

    return String._of("PreparedMetaData{joinPoint: " + joinPoint._string().state + "}");
  }

  @Override
  @Ek9Operator("""
      override operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  @Override
  @Ek9Operator("""
      operator #? as pure
        <- rtn as Integer?""")
  public Integer _hashcode() {
    if (!isSet) {
      return new Integer(); // Return unset Integer for unset state
    }

    // Delegate to JoinPoint hashcode
    return joinPoint._hashcode();
  }

  // Private utility methods

  private void updateSetState() {
    // Fixed: isValid() already checks both null and _isSet().state
    // No need to duplicate the _isSet() check
    if (isValid(joinPoint)) {
      set();
    } else {
      unSet();
    }
  }

  // Public factory methods

  /**
   * Factory method to create a PreparedMetaData with a JoinPoint.
   */
  public static PreparedMetaData _of(JoinPoint joinPoint) {
    return new PreparedMetaData(joinPoint);
  }

  /**
   * Factory method to create a PreparedMetaData from component and method names.
   */
  public static PreparedMetaData _of(java.lang.String componentName, java.lang.String methodName) {
    return new PreparedMetaData(JoinPoint._of(componentName, methodName));
  }
}