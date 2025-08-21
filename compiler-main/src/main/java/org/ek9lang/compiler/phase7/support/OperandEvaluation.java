package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.IRInstr;

/**
 * Record to group operand evaluation instructions with the resulting operand name.
 * <p>
 * This pattern promotes reusability across multiple IR instruction types including
 * LogicalOperationInstr, QuestionOperatorInstr, and future conditional constructs
 * like IF/ELSE and Switch statements.
 * </p>
 * <p>
 * Replaces the flat parameter model with structured grouping of related data:
 * - Instructions that evaluate the operand
 * - Name of the variable holding the operand result
 * </p>
 */
public record OperandEvaluation(
    List<IRInstr> evaluationInstructions,
    String operandName) {

  public OperandEvaluation {
    if (evaluationInstructions == null) {
      throw new IllegalArgumentException("Evaluation instructions cannot be null");
    }
  }
}