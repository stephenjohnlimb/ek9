package org.ek9lang.compiler.phase7.support;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.ek9lang.compiler.ir.instructions.IRInstr;
import org.ek9lang.compiler.ir.instructions.MemoryInstr;

/**
 * CONCERN: Loop counter increment/decrement with assignment.
 * RESPONSIBILITY: Call increment/decrement operator and update loop counter.
 * REUSABILITY: ALL loop generators (for, while with increment).
 * <p>
 * Composite helper that combines:
 * 1. UnaryOperatorInvoker (for ++ or -- operator call)
 * 2. VAR_MOVE assignment (update loop counter)
 * </p>
 * <p>
 * This pattern appears in:
 * - ForStatementGenerator (loop counter increment/decrement)
 * - WhileStatementGenerator (manual increment patterns)
 * </p>
 */
public final class IncrementEvaluator implements Function<IncrementParams, List<IRInstr>> {
  private final UnaryOperatorInvoker unaryOperatorInvoker;

  public IncrementEvaluator(final UnaryOperatorInvoker unaryOperatorInvoker) {
    this.unaryOperatorInvoker = unaryOperatorInvoker;
  }

  /**
   * Evaluate increment/decrement and update counter.
   * <p>
   * Handles:
   * 1. Unary operator invocation (++ or --)
   * 2. VAR_MOVE to update loop counter variable
   * 3. Memory management for increment result
   * </p>
   *
   * @param params Increment parameters
   * @return Instructions ending with updated counter variable
   */
  @Override
  public List<IRInstr> apply(final IncrementParams params) {

    // Step 1: Call increment/decrement operator
    final var unaryParams = new UnaryOperatorParams(
        params.counterVar(),
        params.operator(),
        params.counterType(),
        params.counterType(),
        params.incrementResultTemp(),
        params.scopeId(),
        params.debugInfo()
    );

    final var instructions = new ArrayList<>(unaryOperatorInvoker.apply(unaryParams));

    // Step 2: Update counter variable with incremented/decremented value
    instructions.add(MemoryInstr.store(
        params.counterVar(),
        params.incrementResultTemp(),
        params.debugInfo()
    ));

    return instructions;
  }
}
