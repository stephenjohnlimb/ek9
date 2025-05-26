package org.ek9lang.compiler.phase3;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * Used in Stream assembly processing to check function validity.
 */
record StreamFunctionCheckData(Token errorLocation, FunctionSymbol functionSymbol, ISymbol currentStreamType) {
}
