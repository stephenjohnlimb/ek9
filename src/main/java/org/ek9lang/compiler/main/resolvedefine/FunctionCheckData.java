package org.ek9lang.compiler.main.resolvedefine;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.FunctionSymbol;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * To be used when checking if a function can accept a set of call arguments.
 */
public record FunctionCheckData(Token token, FunctionSymbol function, List<ISymbol> callArgumentTypes) {
}
