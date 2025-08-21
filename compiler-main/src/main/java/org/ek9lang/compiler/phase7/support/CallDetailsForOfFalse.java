package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.ir.CallDetails;

/**
 * Supplies the call details to get a Boolean false value.
 */
public final class CallDetailsForOfFalse implements Supplier<CallDetails> {
  @Override
  public CallDetails get() {
    return new CallDetails("org.ek9.lang::Boolean", "org.ek9.lang::Boolean",
        IRConstants.OF_FALSE_METHOD, List.of(), "org.ek9.lang::Boolean", List.of());
  }
}
