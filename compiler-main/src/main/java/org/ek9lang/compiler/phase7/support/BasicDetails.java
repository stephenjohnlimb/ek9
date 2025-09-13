package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Hold basic debug information.
 * DebugInfo maybe null if debug flag is off.
 * STACK-BASED: scopeId removed - now obtained from IRGenerationContext stack.
 */
public record BasicDetails(DebugInfo debugInfo) {
}