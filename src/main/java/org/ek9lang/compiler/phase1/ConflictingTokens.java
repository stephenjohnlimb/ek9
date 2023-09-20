package org.ek9lang.compiler.phase1;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Designed to encapsulate the fact that two token are in conflict for a Symbol.
 * Quite abstract in nature, but may have a few use cases.
 */
record ConflictingTokens(IToken tokenInError, IToken firstUse, ISymbol symbol) {

}
