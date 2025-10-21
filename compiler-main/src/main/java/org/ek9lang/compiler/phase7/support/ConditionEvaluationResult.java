package org.ek9lang.compiler.phase7.support;

import java.util.List;
import org.ek9lang.compiler.ir.instructions.IRInstr;

/**
 * Result of condition evaluation with primitive boolean extraction.
 * <p>
 * CONCERN: Structured return value for condition evaluation operations.
 * RESPONSIBILITY: Eliminate fragile IR pattern matching to extract primitive variable names.
 * REUSABILITY: All control flow generators needing condition evaluation results.
 * </p>
 * <p>
 * Replaces the fragile {@code extractPrimitiveVariable(List<IRInstr>)} pattern
 * which does string matching on IR instruction sequences. Instead, methods return
 * structured results with both IR instructions and the primitive variable name.
 * </p>
 * <p>
 * <b>Pattern replaced:</b>
 * </p>
 * <pre>
 * final var instructions = generateCondition(...);
 * final var primitiveVar = extractPrimitiveVariable(instructions);  // ← FRAGILE
 * </pre>
 * <p>
 * <b>New pattern:</b>
 * </p>
 * <pre>
 * final var result = generateCondition(...);
 * final var instructions = result.instructions();
 * final var primitiveVar = result.primitiveVariableName();  // ← TYPE-SAFE
 * </pre>
 *
 * @param instructions IR instruction sequence for condition evaluation (scoped)
 * @param primitiveVariableName Name of primitive boolean variable for branching
 */
public record ConditionEvaluationResult(
    List<IRInstr> instructions,
    String primitiveVariableName
) {
}
