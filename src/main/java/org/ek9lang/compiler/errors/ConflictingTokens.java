package org.ek9lang.compiler.errors;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Designed to encapsulate the fact that two token are in conflict for a Symbol.
 * Quite abstract in nature, but may have a few use cases.
 */
public record ConflictingTokens(Token tokenInError, Token firstUse, ISymbol symbol) {

}
