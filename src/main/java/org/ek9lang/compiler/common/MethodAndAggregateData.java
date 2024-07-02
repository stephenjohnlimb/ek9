package org.ek9lang.compiler.common;

import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;

/**
 * Useful when attempting to traverse up stacks to find both the method and its parent aggregate.
 */
public record MethodAndAggregateData(MethodSymbol methodSymbol, IAggregateSymbol aggregateSymbol) {
}
