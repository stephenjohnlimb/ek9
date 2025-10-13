package org.ek9lang.compiler.phase7.support;

import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.core.AssertValue;

/**
 * Triggers the expression processing with the given details.
 * Then ensures that the exprResult variable is retained and tracked with the appropriate scope.
 * This means that any use of this will both retain the variable and schedule it with the scope for ARC
 * decrement (and possible deletion).
 * <p>
 * So typically this is only used with local variables and internal temporary variables.
 * STACK-BASED: Uses IRGenerationContext to get current scope ID from stack.
 * </p>
 */
public class RecordExprProcessing implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final Function<ExprProcessingDetails, List<IRInstr>> processor;
  private final VariableMemoryManagement variableMemoryManagement;

  public RecordExprProcessing(final Function<ExprProcessingDetails, List<IRInstr>> processor,
                              final VariableMemoryManagement variableMemoryManagement) {
    AssertValue.checkNotNull("Processor cannot be null", processor);
    AssertValue.checkNotNull("VariableMemoryManagement cannot be null", variableMemoryManagement);
    this.processor = processor;
    this.variableMemoryManagement = variableMemoryManagement;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {

    return variableMemoryManagement.apply(() -> processor.apply(details), details.variableDetails());

  }
}
