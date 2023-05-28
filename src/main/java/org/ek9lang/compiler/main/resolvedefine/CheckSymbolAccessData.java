package org.ek9lang.compiler.main.resolvedefine;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.IScope;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Used when checking access to fields/properties and methods.
 */
public record CheckSymbolAccessData(Token token, IScope fromScope, IScope inScope, String symbolName, ISymbol symbol) {
}
