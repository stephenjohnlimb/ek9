package org.ek9lang.compiler.phase7.support;

import org.ek9lang.compiler.ir.support.DebugInfo;

/**
 * Just holds details for retaining and registering a target for memory management.
 * STACK-BASED: Now uses DebugInfo directly instead of BasicDetails wrapper.
 */
public record VariableDetails(String resultVariable, DebugInfo debugInfo) {
}
