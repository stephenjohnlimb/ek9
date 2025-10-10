package org.ek9lang.compiler.phase7.calls;

import java.util.List;
import java.util.function.BiFunction;
import org.ek9lang.compiler.common.OperatorMap;
import org.ek9lang.compiler.ir.data.CallDetails;
import org.ek9lang.compiler.ir.data.CallMetaDataDetails;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Creates CallDetails for _isSet() method invocation.
 * The _isSet() operator (?) checks if a value is set/valid.
 * This is a pure function method that returns an EK9 Boolean.
 */
public final class IsSetCallDetailsCreator implements BiFunction<String, String, CallDetails> {
  private final OperatorMap operatorMap = new OperatorMap();

  @Override
  public CallDetails apply(final String operandVariable, final String operandType) {
    final var methodName = operatorMap.getForward("?");
    final var isSetMetaData = new CallMetaDataDetails(true, 0);

    return new CallDetails(
        operandVariable,
        operandType,
        methodName,
        List.of(),
        EK9TypeNames.EK9_BOOLEAN,
        List.of(),
        isSetMetaData,
        false);
  }
}
