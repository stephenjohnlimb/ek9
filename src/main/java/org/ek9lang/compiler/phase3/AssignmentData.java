package org.ek9lang.compiler.phase3;

/**
 * Used to decouple checks from the grammar contexts.
 */
record AssignmentData(boolean isAssigningToThis,
                             TypeCompatibilityData typeData) {
}
