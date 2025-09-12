package org.ek9lang.compiler.phase7.support;

import org.ek9lang.antlr.EK9Parser;
import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Processing details for primary reference generation (THIS and SUPER keywords).
 */
public record PrimaryReferenceProcessingDetails(
    EK9Parser.PrimaryReferenceContext ctx,
    String resultVariable,
    DebugInfo debugInfo
) {
}