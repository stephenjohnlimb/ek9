package org.ek9lang.compiler.ir;

import org.ek9lang.compiler.phase7.support.BasicDetails;
import org.ek9lang.compiler.phase7.support.OperandEvaluation;

/**
 * Just holds the details relating to the 'is set' functionality.
 * While there is an operator '_isSet' (?), the same concept is used elsewhere.
 * For example in guard assignment statements and also in guarded if/for/while etc.
 */
public record QuestionDetails(String result,
                              OperandEvaluation operandEvaluation,
                              String nullCheckCondition,
                              OperandEvaluation nullCaseEvaluation,
                              OperandEvaluation setCaseEvaluation,
                              BasicDetails basicDetails) {
}
