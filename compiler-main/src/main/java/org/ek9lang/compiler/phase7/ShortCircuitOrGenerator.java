package org.ek9lang.compiler.phase7;

import java.util.List;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.LogicalOperationInstr;
import org.ek9lang.compiler.phase7.support.IRContext;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Generates IR instructions for Boolean OR operations using LOGICAL_OR_BLOCK pattern.
 * <p>
 * This generator creates a declarative logical operation block containing:
 * 1. Left operand evaluation and primitive boolean condition
 * 2. Right operand evaluation instructions (for non-short-circuit path)
 * 3. Result computation instructions (EK9 Boolean._or() call)
 * 4. All memory management
 * </p>
 * <p>
 * Backends can choose between short-circuit and full evaluation strategies
 * based on the usage context and target-specific optimizations.
 * </p>
 */
public final class ShortCircuitOrGenerator extends AbstractShortCircuitGenerator {

  public ShortCircuitOrGenerator(final IRContext context,
                                 final RecordExprProcessing recordExprProcessing) {
    super(context, recordExprProcessing, LogicalOperationInstr::orOperation);
  }

  @Override
  protected CallDetails getCallDetails(final String lhsVariable, final String rhsVariable) {
    final var booleanType = EK9TypeNames.EK9_BOOLEAN;
    return new CallDetails(lhsVariable, booleanType,
        "_or", List.of(booleanType),
        booleanType, List.of(rhsVariable), new CallMetaData(true, 0));
  }

}