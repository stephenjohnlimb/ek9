package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.ir.CallDetails;

/**
 * All the focus on calling a method on Boolean that takes no arguments are returns a Boolean.
 * This is quite a common use (hence this class).
 */
public class CallDetailsOnBoolean implements BiFunction<String, String, CallDetails> {
  @Override
  public CallDetails apply(final String targetObject, final String methodName) {
    return new CallDetails(targetObject, "org.ek9.lang::Boolean",
        methodName, List.of(), "org.ek9.lang::Boolean", List.of());
  }
}
