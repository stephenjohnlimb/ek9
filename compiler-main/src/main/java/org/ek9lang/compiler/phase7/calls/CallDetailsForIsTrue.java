package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Just provides the IR Call details for making a call to the Ek9 Boolean type to call the _true() method.
 * This returns a primitive boolean in the backend implementation.
 * For Java, it will be a 'boolean', for LLVM probably an i8 (1 or 0 value).
 */
public final class CallDetailsForIsTrue implements Function<String, CallDetails> {

  @Override
  public CallDetails apply(final String targetObject) {
    return new CallDetails(targetObject, EK9TypeNames.EK9_BOOLEAN,
        IRConstants.TRUE_METHOD, List.of(), IRConstants.BOOLEAN, List.of(),
        new CallMetaData(true, 0));
  }
}
