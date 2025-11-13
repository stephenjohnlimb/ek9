package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Provides IR Call details for making a call to the EK9 Boolean type to call the _false() method.
 * This returns a primitive boolean in the backend implementation.
 * Returns the inverted primitive: true if Boolean is false, false if Boolean is true.
 * For Java, it will be a 'boolean', for LLVM probably an i8 (1 or 0 value).
 */
public final class CallDetailsForIsFalse implements Function<String, CallDetails> {

  @Override
  public CallDetails apply(final String targetObject) {
    return new CallDetails(targetObject, EK9TypeNames.EK9_BOOLEAN,
        IRConstants.FALSE_METHOD, List.of(), IRConstants.BOOLEAN, List.of(),
        new CallMetaDataDetails(true, 0), false);
  }
}
