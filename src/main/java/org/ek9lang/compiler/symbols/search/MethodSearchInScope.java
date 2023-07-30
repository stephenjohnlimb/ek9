package org.ek9lang.compiler.symbols.search;

import org.ek9lang.compiler.symbols.IScope;

/**
 * Simple record tuple to combine the scope to be searched and the method signature to be search for.
 */
public record MethodSearchInScope(IScope scopeToSearch, MethodSymbolSearch search) {
}
