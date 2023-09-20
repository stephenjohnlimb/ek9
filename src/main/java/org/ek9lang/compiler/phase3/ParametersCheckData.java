package org.ek9lang.compiler.phase3;

import java.util.List;
import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * Used for checking parameter list types match.
 * Typically for overloading Function, operations.
 * This uses an exact match - because there is overloading.
 * This is unlike the match that is done when calls are made, in that situation you are looking
 * for type compatibility and also include coercion.
 */
record ParametersCheckData(IToken token, String errorMessage, List<ISymbol> from, List<ISymbol> to) {
}
