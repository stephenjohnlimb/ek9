package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import java.util.function.Supplier;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.phase7.support.IRConstants;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Supplies the call details to get a Boolean false value.
 */
public final class CallDetailsForOfFalse implements Supplier<CallDetails> {
  @Override
  public CallDetails get() {
    final var booleanType = EK9TypeNames.EK9_BOOLEAN;
    return new CallDetails(booleanType, booleanType,
        IRConstants.OF_FALSE_METHOD, List.of(), booleanType, List.of(),
        new CallMetaDataDetails(true, 0), false);
  }
}
