package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import org.ek9lang.compiler.ir.CallDetails;
import org.ek9lang.compiler.ir.CallMetaData;
import org.ek9lang.compiler.ir.LogicalOperationInstr;
import org.ek9lang.compiler.phase7.generation.IRGenerationContext;
import org.ek9lang.compiler.phase7.support.RecordExprProcessing;
import org.ek9lang.compiler.support.EK9TypeNames;

/**
 * Generates IR instructions for Boolean AND operations using LOGICAL_AND_BLOCK pattern.
 * <p>
 * This generator creates a declarative logical operation block containing:<br>
 * 1. Left operand evaluation and primitive boolean condition<br>
 * 2. Right operand evaluation instructions (for non-short-circuit path)<br>
 * 3. Result computation instructions (EK9 Boolean._and() call)<br>
 * 4. All memory management<br>
 * </p>
 * <p>
 * Backends can choose between short-circuit and full evaluation strategies
 * based on the usage context and target-specific optimizations.
 * </p>
 */
public final class ShortCircuitAndGenerator extends AbstractShortCircuitGenerator {

  public ShortCircuitAndGenerator(final IRGenerationContext stackContext,
                                  final RecordExprProcessing recordExprProcessing) {
    super(stackContext, recordExprProcessing, LogicalOperationInstr::andOperation);
  }

  @Override
  protected CallDetails getCallDetails(final String lhsVariable, final String rhsVariable) {
    final var booleanType = EK9TypeNames.EK9_BOOLEAN;
    return new CallDetails(lhsVariable, booleanType,
        "_and", List.of(booleanType),
        booleanType, List.of(rhsVariable), new CallMetaData(true, 0));
  }

}