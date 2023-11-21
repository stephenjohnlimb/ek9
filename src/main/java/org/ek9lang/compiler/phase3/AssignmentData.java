package org.ek9lang.compiler.phase3;

/**
 * Used to decouple checks from the grammar contexts.
 */
public record AssignmentData(boolean isAssigningToThis,
                             TypeCompatibilityData typeData) {
}
