package org.ek9.lang;

import org.ek9tooling.Ek9Class;
import org.ek9tooling.Ek9Constructor;
import org.ek9tooling.Ek9Method;
import org.ek9tooling.Ek9Operator;

/**
 * EK9 Aspect base class for aspect-oriented programming.
 * <p>
 * Aspect is a stateless utility class that is always set - it contains no properties
 * and serves as a base class for concrete aspects that implement cross-cutting concerns
 * like logging, timing, security, etc.
 * </p>
 * <p>
 * Concrete aspects extend this class and override the beforeAdvice() and afterAdvice()
 * methods to implement specific cross-cutting behaviors.
 * </p>
 * <p>
 * Example usage in EK9:
 * </p>
 * <pre>
 * LoggingAspect extends Aspect
 *   override beforeAdvice()
 *     -> joinPoint as JoinPoint
 *     <- rtn as PreparedMetaData: PreparedMetaData(joinPoint)
 *     // Log before method execution
 *
 *   override afterAdvice()
 *     -> preparedMetaData as PreparedMetaData
 *     // Log after method execution
 * </pre>
 */
@SuppressWarnings("checkstyle:MethodName")
@Ek9Class("""
    Aspect as open""")
public class Aspect extends BuiltinType {

  @Ek9Constructor("""
      Aspect() as pure""")
  public Aspect() {
    set(); // Always set since Aspect is stateless utility
  }

  @Ek9Method("""
      beforeAdvice()
        -> joinPoint as JoinPoint
        <- rtn as PreparedMetaData?""")
  public PreparedMetaData beforeAdvice(JoinPoint joinPoint) {
    // Base implementation: create PreparedMetaData from JoinPoint
    // Concrete aspects can override this method for specific behaviors
    if (isValid(joinPoint)) {
      return new PreparedMetaData(joinPoint);
    }
    return new PreparedMetaData(); // Return unset PreparedMetaData for invalid input
  }

  @Ek9Method("""
      afterAdvice() as dispatcher
        -> preparedMetaData as PreparedMetaData""")
  public void afterAdvice(PreparedMetaData preparedMetaData) {
    // Base implementation: no-op dispatcher method
    // Concrete aspects can override this method for specific cleanup/logging
    // Dispatcher methods are void and can be overloaded by concrete types
  }

  @Override
  @Ek9Operator("""
      operator ? as pure
        <- rtn as Boolean?""")
  public Boolean _isSet() {
    return Boolean._of(true); // Always set - Aspect is stateless utility
  }

  // Optional string representation for debugging (not in interface but useful)
  @Override
  public String _string() {
    return String._of("Aspect{}");
  }
}