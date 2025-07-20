package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 JoinPoint type that represents a point of execution with component and method name.
 * <p>
 * JoinPoint is set only when both componentName and methodName are set and valid.
 * This makes it useful for aspect-oriented programming and debugging contexts.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>
 * joinPoint &lt;- JoinPoint("MyClass", "myMethod")
 * if joinPoint?
 *   Stdout().println(joinPoint)
 * </pre>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    JoinPoint as open""")
public class JoinPoint extends BuiltinType {

  private String componentName = new String();
  private String methodName = new String();

  @Ek9Constructor("""
      JoinPoint() as pure""")
  public JoinPoint() {
    // Default constructor creates unset JoinPoint
    unSet();
  }

  @Ek9Constructor("""
      JoinPoint() as pure
        ->
          componentName as String
          methodName as String""")
  public JoinPoint(String componentName, String methodName) {
    this.componentName = isValid(componentName) ? new String(componentName) : new String();
    this.methodName = isValid(methodName) ? new String(methodName) : new String();
    updateSetState();
  }

  @Ek9Method("""
      componentName() as pure
        <- rtn as String?""")
  public String componentName() {
    return isSet ? new String(componentName) : new String();
  }

  @Ek9Method("""
      methodName() as pure
        <- rtn as String?""")
  public String methodName() {
    return isSet ? new String(methodName) : new String();
  }

  @Override
  @Ek9Operator("""
      operator == as pure
        -> arg as Any
        <- rtn as Boolean?""")
  public Boolean _eq(Any arg) {
    if (arg instanceof JoinPoint asJoinPoint) {
      return _eq(asJoinPoint);
    }
    return new Boolean(); // Return unset for non-JoinPoint comparison
  }

  @Ek9Operator("""
      operator == as pure
        -> arg as JoinPoint
        <- rtn as Boolean?""")
  public Boolean _eq(JoinPoint arg) {
    if (!isValid(arg)) {
      return new Boolean(); // Return unset for invalid comparison
    }

    // Both must be set to compare
    if (!isSet || !arg.isSet) {
      return new Boolean();
    }

    boolean componentEqual = this.componentName._eq(arg.componentName).state;
    boolean methodEqual = this.methodName._eq(arg.methodName).state;
    return Boolean._of(componentEqual && methodEqual);
  }

  @Ek9Operator("""
      operator <> as pure
        -> arg as JoinPoint
        <- rtn as Boolean?""")
  public Boolean _neq(JoinPoint arg) {
    return _eq(arg)._negate();
  }

  @Ek9Operator("""
      operator <=> as pure
        -> arg as JoinPoint
        <- rtn as Integer?""")
  public Integer _cmp(JoinPoint arg) {
    if (!isValid(arg) || !isSet || !arg.isSet) {
      return new Integer(); // Return unset for invalid comparison
    }

    // Compare componentName first
    Integer componentCmp = this.componentName._cmp(arg.componentName);
    if (!componentCmp._isSet().state) {
      return new Integer();
    }

    if (componentCmp.state != 0) {
      return componentCmp;
    }

    // If componentName equal, compare methodName
    return this.methodName._cmp(arg.methodName);
  }

  @Override
  @Ek9Operator("""
      operator <=> as pure
        -> arg as Any
        <- rtn as Integer?""")
  public Integer _cmp(Any arg) {
    if (arg instanceof JoinPoint asJoinPoint) {
      return _cmp(asJoinPoint);
    }
    return new Integer(); // Return unset for non-JoinPoint comparison
  }

  @Ek9Operator("""
      operator :~:
        -> arg as JoinPoint""")
  public void _merge(JoinPoint arg) {
    if (!isSet && isValid(arg)) {
      _copy(arg);
    }
  }

  @Ek9Operator("""
      operator :^:
        -> arg as JoinPoint""")
  public void _replace(JoinPoint arg) {
    _copy(arg); // Replace delegates to copy
  }

  @Ek9Operator("""
      operator :=:
        -> arg as JoinPoint""")
  public void _copy(JoinPoint arg) {
    if (isValid(arg)) {
      this.componentName._copy(arg.componentName);
      this.methodName._copy(arg.methodName);
      updateSetState();
    } else {
      this.componentName = new String();
      this.methodName = new String();
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
      return String._of("JoinPoint{}");
    }

    return String._of(
        "JoinPoint{componentName: '" + componentName.state + "', methodName: '" + methodName.state + "'}");
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
    if (!isSet) {
      return new Integer();
    }

    int componentHash = (int) componentName._hashcode().state;
    int methodHash = (int) methodName._hashcode().state;
    return Integer._of(31 * componentHash + methodHash);
  }

  // Private utility methods

  private void updateSetState() {
    if (isValid(componentName) && componentName._isSet().state
        && isValid(methodName) && methodName._isSet().state) {
      set();
    } else {
      unSet();
    }
  }

  // Public factory methods (following GUID pattern)

  /**
   * Factory method to create a JoinPoint with both component and method names.
   */
  public static JoinPoint _of(String componentName, String methodName) {
    return new JoinPoint(componentName, methodName);
  }

  /**
   * Factory method to create a JoinPoint from Java strings.
   */
  public static JoinPoint _of(java.lang.String componentName, java.lang.String methodName) {
    return new JoinPoint(String._of(componentName), String._of(methodName));
  }

}