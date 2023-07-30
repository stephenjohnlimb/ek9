package org.ek9lang.compiler.phase3;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * To be used when checking if a function can accept a set of call arguments.
 */
record FunctionCheckData(Token token, FunctionSymbol function, List<ISymbol> callArgumentTypes) {
}
