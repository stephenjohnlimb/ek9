package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.DebugInfo;

/**
 * Just hold basic scope and debug information.
 * DebugInfo maybe null if debug flag is off.
 */
public record BasicDetails(String scopeId,
                           DebugInfo debugInfo) {
}
