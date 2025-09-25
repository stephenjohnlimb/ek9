package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.MethodSymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Data structure to facilitate checks on methods on an aggregate.
 */
record AggregateMethodData(IToken location, IAggregateSymbol aggregate, MethodSymbol methodSymbol) {
}
