package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.IRInstr;

/**
 * Triggers the expression processing with the given details.
 * Then ensures that the exprResult variable is retained and tracked with the appropriate scope.
 * This means that any use of this will both retain the variable and schedule it with the scope for ARC
 * decrement (and possible deletion).
 * <p>
 * So typically this is only used with local variables and internal temporary variables.
 * </p>
 */
public class RecordExprProcessing implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final Function<ExprProcessingDetails, List<IRInstr>> processor;
  private final VariableMemoryManagement variableMemoryManagement = new VariableMemoryManagement();

  public RecordExprProcessing(final Function<ExprProcessingDetails, List<IRInstr>> processor) {
    this.processor = processor;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {

    return variableMemoryManagement.apply(() -> processor.apply(details), details.variableDetails());

  }
}
