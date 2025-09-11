package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Record to group conditional evaluation instructions with the resulting condition name.
 * <p>
 * This pattern supports future IF/ELSE/Switch chaining by providing a reusable
 * structure for condition evaluation across multiple IR instruction types:
 * - LogicalOperationInstr (primitive boolean conditions)
 * - GuardedAssignmentBlockInstr (assignment conditions)
 * - Future IfElseBlockInstr (branching conditions)
 * - Future SwitchBlockInstr (switch conditions)
 * </p>
 * <p>
 * Replaces the flat parameter model with structured grouping of related data:
 * - Instructions that evaluate the condition
 * - Name of the variable holding the condition result
 * </p>
 */
public record ConditionalEvaluation(
    List<IRInstr> conditionInstructions,
    String conditionResult) {

  public ConditionalEvaluation {
    if (conditionInstructions == null) {
      throw new IllegalArgumentException("Condition instructions cannot be null");
    }
    if (conditionResult == null || conditionResult.isBlank()) {
      throw new IllegalArgumentException("Condition result cannot be null or blank");
    }
  }
}