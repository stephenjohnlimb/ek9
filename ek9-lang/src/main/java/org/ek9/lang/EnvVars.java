package org.ek9.lang;

import java.util.Iterator;
import java.util.Set;
import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 EnvVars type that provides environment variable access.
 * This is a stateless utility class - it holds no state and isSet is always true.
 * Provides access to environment variables and their keys.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    EnvVars""")
public class EnvVars extends BuiltinType {

  @Ek9Constructor("""
      EnvVars() as pure""")
  public EnvVars() {
    set(); // Always set since EnvVars is stateless
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      keys() as pure
        <- rtn as StringInput?""")
  public StringInput keys() {
    try {
      Set<java.lang.String> envKeys = System.getenv().keySet();
      return new EnvVarsStringInput(envKeys);
    } catch (Exception _) {
      return new EnvVarsStringInput(null); // Return unset StringInput on error
    }
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      get() as pure
        -> environmentVariableName as String
        <- environmentVariableValue as String?""")
  public String get(String environmentVariableName) {
    if (!isValid(environmentVariableName)) {
      return new String(); // Return unset String for invalid input
    }

    try {
      java.lang.String envValue = System.getenv(environmentVariableName.state);
      if (envValue == null) {
        return new String(); // Return unset String for non-existent env var
      }
      return String._of(envValue);
    } catch (Exception _) {
      return new String(); // Return unset String on error
    }
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Operator("""
      operator contains as pure
        -> environmentVariableName as String
        <- rtn as Boolean?""")
  public Boolean _contains(String environmentVariableName) {
    if (!isValid(environmentVariableName)) {
      return new Boolean(); // Return unset Boolean for invalid input
    }

    try {
      boolean exists = System.getenv().containsKey(environmentVariableName.state);
      return Boolean._of(exists);
    } catch (Exception _) {
      return new Boolean(); // Return unset Boolean on error
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }

  /**
   * StringInput implementation for environment variable keys.
   * Provides iteration over environment variable key names.
   */
  private static class EnvVarsStringInput extends BuiltinType implements StringInput {
    private final Iterator<java.lang.String> iterator;
    private boolean closed = false;

    EnvVarsStringInput(Set<java.lang.String> envKeys) {
      if (envKeys == null) {
        iterator = null;
        unSet(); // Mark as unset for invalid input
      } else {
        iterator = envKeys.iterator();
        set(); // Mark as set for valid input
      }
    }

    @SuppressWarnings("checkstyle:CatchParameterName")
    @Override
    public String next() {
      if (closed || iterator == null || !iterator.hasNext()) {
        return new String(); // Return unset String when no more elements
      }

      try {
        java.lang.String nextKey = iterator.next();
        return String._of(nextKey);
      } catch (Exception _) {
        return new String(); // Return unset String on error
      }
    }

    @SuppressWarnings("checkstyle:CatchParameterName")
    @Override
    public Boolean hasNext() {
      if (closed || iterator == null) {
        return Boolean._of(false);
      }

      try {
        return Boolean._of(iterator.hasNext());
      } catch (Exception _) {
        return Boolean._of(false);
      }
    }

    @Override
    public void _close() {
      if (!closed) {
        closed = true;
        // No resources to close for environment variables
      }
    }

    @Override
    public Boolean _isSet() {
      return Boolean._of(isSet && !closed);
    }
  }
}