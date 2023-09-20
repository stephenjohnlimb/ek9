package org.ek9lang.compiler.support;

import java.util.List;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * To be used when trying to define/resolve a parameterised type.
 */
public record ParameterisedTypeData(IToken location, ISymbol genericTypeSymbol, List<ISymbol> typeArguments) {
}
