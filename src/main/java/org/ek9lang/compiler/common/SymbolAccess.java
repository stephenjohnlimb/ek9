package org.ek9lang.compiler.common;

import java.util.Set;

/**
 * Models information used in code analysis for variable variable states.
 */
public record SymbolAccess(Set<String> metaData) {
}
