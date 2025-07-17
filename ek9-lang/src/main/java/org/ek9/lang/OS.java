package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 OS type that provides operating system information.
 * This is a stateless utility class - it holds no state and isSet is always true.
 * Provides process ID and other OS-related information.
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    OS""")
public class OS extends BuiltinType {

  @Ek9Constructor("""
      OS() as pure""")
  public OS() {
    set(); // Always set since OS is stateless
  }

  @SuppressWarnings("checkstyle:CatchParameterName")
  @Ek9Method("""
      pid() as pure
        <- rtn as Integer?""")
  public Integer pid() {
    try {
      long pid = ProcessHandle.current().pid();
      
      // Handle potential overflow from long to int
      if (pid > java.lang.Integer.MAX_VALUE || pid < 0) {
        return new Integer(); // Return unset for overflow or invalid PID
      }
      
      return Integer._of((int) pid);
    } catch (Exception _) {
      return new Integer(); // Return unset Integer on error
    }
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(isSet);
  }
}