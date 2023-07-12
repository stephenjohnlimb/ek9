package org.ek9lang.compiler.main.rules;

import java.util.List;
import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbol.ISymbol;

/**
 * Used for checking parameter list types match.
 * Typically for overloading Function, operations.
 * This uses an exact match - because there is overloading.
 * This is unlike the match that is done when calls are made, in that situation you are looking
 * for type compatibility and also include coercion.
 */
public record ParametersCheckData(Token token, String errorMessage, List<ISymbol> from, List<ISymbol> to) {
}
