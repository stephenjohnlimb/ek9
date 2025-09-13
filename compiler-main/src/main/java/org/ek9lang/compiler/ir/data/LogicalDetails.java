package org.ek9lang.compiler.ir.data;

import org.ek9lang.compiler.ir.support.DebugInfo;
import org.ek9lang.compiler.phase7.support.ConditionalEvaluation;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;

/**
 * Defines the necessary structure for processing logical operations line AND/OR/XOR.
 * STACK-BASED: scopeId extracted from stack context at Details creation time.
 */
public record LogicalDetails(String result,
                             OperandEvaluation leftEvaluation,
                             ConditionalEvaluation conditionalEvaluation,
                             OperandEvaluation rightEvaluation,
                             OperandEvaluation resultEvaluation,
                             DebugInfo debugInfo,
                             String scopeId) {
}
