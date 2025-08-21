package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.IRInstr;
import org.ek9lang.compiler.ir.MemoryInstr;
import org.ek9lang.compiler.ir.ScopeInstr;

/**
 * Triggers the expression processing with the given details.
 * Then ensures that the exprResult variable is retained and tracked with the appropriate scope.
 * This means that any ise of this will both retain the variable and schedule it with the scope for ARC
 * decrement (and possible deletion).
 * <p>
 * So typically this is only used with local variables and internal temporary variables.
 * </p>
 */
public class RecordExprProcessing implements Function<ExprProcessingDetails, List<IRInstr>> {

  private final Function<ExprProcessingDetails, List<IRInstr>> processor;

  public RecordExprProcessing(final Function<ExprProcessingDetails, List<IRInstr>> processor) {
    this.processor = processor;
  }

  @Override
  public List<IRInstr> apply(final ExprProcessingDetails details) {

    final var instructions = new ArrayList<>(processor.apply(details));

    // Register temp for proper memory management and exception safety
    instructions.add(MemoryInstr.retain(details.exprResult(), details.basicDetails().debugInfo()));
    instructions.add(ScopeInstr.register(details.exprResult(), details.basicDetails()));
    return instructions;

  }
}
