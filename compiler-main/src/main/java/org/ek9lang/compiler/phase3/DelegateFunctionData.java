package org.ek9lang.compiler.phase3;

import java.util.List;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * To be used when checking if a variable Symbol is a function delegate and can accept a set of call arguments.
 */
record DelegateFunctionData(IToken token, ISymbol delegateSymbol, List<ISymbol> callArgumentTypes) {
}
