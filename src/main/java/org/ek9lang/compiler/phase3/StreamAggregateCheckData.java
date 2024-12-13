package org.ek9lang.compiler.phase3;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.IAggregateSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used in Stream assembly processing to check aggregate use validity.
 */
record StreamAggregateCheckData(Token errorLocation,
                                IAggregateSymbol aggregateSymbol,
                                ISymbol symbolType) {
}
