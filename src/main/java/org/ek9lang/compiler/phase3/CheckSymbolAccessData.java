package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Used when checking access to fields/properties and methods.
 */
record CheckSymbolAccessData(IToken token, IScope fromScope, IScope inScope, String symbolName, ISymbol symbol) {
}
