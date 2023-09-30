package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.search.MethodSymbolSearch;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

record CheckOperatorData(ISymbol symbol, IToken operatorUseToken, MethodSymbolSearch search) {
}
