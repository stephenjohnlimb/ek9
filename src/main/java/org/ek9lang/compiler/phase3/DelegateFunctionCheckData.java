package org.ek9lang.compiler.phase3;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * To be used when checking if a variable Symbol is a function delegate and can accept a set of call arguments.
 */
record DelegateFunctionCheckData(Token token, ISymbol delegateSymbol, List<ISymbol> callArgumentTypes) {
}
