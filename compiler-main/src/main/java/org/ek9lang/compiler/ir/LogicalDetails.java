package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;

/**
 * Defines the necessary structure for processing logical operations line AND/OR/XOR.
 */
public record LogicalDetails(String result,
                             OperandEvaluation leftEvaluation,
                             ConditionalEvaluation conditionalEvaluation,
                             OperandEvaluation rightEvaluation,
                             OperandEvaluation resultEvaluation,
                             BasicDetails basicDetails) {
}
