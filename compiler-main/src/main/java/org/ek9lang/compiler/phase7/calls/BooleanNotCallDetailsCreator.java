package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Creates CallDetails for Boolean._not() method invocation.
 * The _not() operator (~) inverts a boolean value.
 * This is a pure function method that returns an EK9 Boolean.
 */
public final class BooleanNotCallDetailsCreator implements Function<String, CallDetails> {
  private final OperatorMap operatorMap = new OperatorMap();

  @Override
  public CallDetails apply(final String booleanVariable) {
    final var booleanType = EK9TypeNames.EK9_BOOLEAN;
    final var methodName = operatorMap.getForward("~");
    final var notMetaData = new CallMetaDataDetails(true, 0);

    return new CallDetails(
        booleanVariable,
        booleanType,
        methodName,
        List.of(),
        booleanType,
        List.of(),
        notMetaData,
        false);
  }
}
