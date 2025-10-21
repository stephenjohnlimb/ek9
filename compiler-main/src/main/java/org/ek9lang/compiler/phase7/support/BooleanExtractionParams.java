package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Parameters for boolean primitive extraction.
 * <p>
 * Encapsulates all data needed to extract a primitive boolean from
 * an EK9 Boolean object with proper memory management.
 * </p>
 *
 * @param booleanObjectVar Variable name holding the EK9 Boolean object
 * @param resultTemp Variable name to store the primitive boolean result
 * @param debugInfo Debug information for instructions
 */
public record BooleanExtractionParams(
    String booleanObjectVar,
    String resultTemp,
    DebugInfo debugInfo
) {
}
