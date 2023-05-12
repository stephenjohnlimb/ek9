package org.ek9lang.compiler.symbol.support.search;

import org.ek9lang.compiler.symbol.IAggregateSymbol;

/**
 * Simple record tuple to combine the aggregate to be search and the method signature to be search for.
 */
public record MethodSearchOnAggregate(IAggregateSymbol aggregate, MethodSymbolSearch search) {
}
