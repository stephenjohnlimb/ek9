package org.ek9lang.compiler.phase7.generator;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Result of evaluating a condition expression (with optional guard variables).
 * <p>
 * Contains both the IR instructions for evaluating the condition and
 * the name of the primitive boolean variable that the backend can use
 * for branching instructions (IFEQ, IFNE, etc.).
 * </p>
 * <p>
 * This is returned by {@link GuardedConditionEvaluator} after processing
 * any of the 4 condition/guard combinations.
 * </p>
 *
 * @param instructions      The IR instructions that evaluate the condition
 *                          (includes LOGICAL_AND_BLOCK for guards, or simple
 *                          expression evaluation, or QUESTION_OPERATOR)
 * @param primitiveCondition The name of the primitive boolean variable (e.g., "_temp11")
 *                          that contains the final boolean result for backend branching
 */
public record ConditionEvaluation(
    List<IRInstr> instructions,
    String primitiveCondition) {
}
