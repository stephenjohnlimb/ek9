package org.ek9lang.compiler.common;

/**
 * Holds some high level details about an operator.
 * These by their very nature cannot be too detailed, because when used the actual types involved
 * will vary.
 * But we do need to try and gather together the nature of operators.
 * See OperatorMap for use.
 * <p>
 * Also when generating the IR we need to know about purity and if a return is present.
 * This aids in providing meta-data to the IR phase for optimisation and memory management.
 * </p>
 */
public record OperatorDetails(String operator,
                              String mappedName,
                              boolean markedPure,
                              boolean requiresArgument,
                              boolean hasReturn) {
}
