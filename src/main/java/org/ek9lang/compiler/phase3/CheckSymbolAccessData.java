package org.ek9lang.compiler.phase3;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.IScope;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used when checking access to fields/properties and methods.
 */
record CheckSymbolAccessData(Token token, IScope fromScope, IScope inScope, String symbolName, ISymbol symbol) {
}
