package org.ek9lang.compiler.phase7.support;

import org.ek9lang.antlr.EK9Parser;

/**
 * Processing details for primary reference generation (THIS and SUPER keywords).
 * <p>
 * MIGRATING TO STACK: DebugInfo removed since PrimaryReferenceGenerator now creates
 * debug info from stack context instead of parameter threading.
 * </p>
 */
public record PrimaryReferenceProcessingDetails(
    EK9Parser.PrimaryReferenceContext ctx,
    String resultVariable
) {
}