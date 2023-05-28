package org.ek9lang.compiler.symbol.support.search;

import org.ek9lang.compiler.symbol.IScope;

/**
 * Simple record tuple to combine the scope to be searched and the method signature to be search for.
 */
public record MethodSearchInScope(IScope scopeToSearch, MethodSymbolSearch search) {
}
