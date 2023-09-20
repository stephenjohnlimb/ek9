package org.ek9lang.compiler.phase3;

import java.util.List;
import org.ek9lang.compiler.symbols.FunctionSymbol;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * To be used when checking if a function can accept a set of call arguments.
 */
record FunctionCheckData(IToken token, FunctionSymbol function, List<ISymbol> callArgumentTypes) {
}
