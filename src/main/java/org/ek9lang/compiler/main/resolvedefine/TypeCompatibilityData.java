package org.ek9lang.compiler.main.resolvedefine;

import org.antlr.v4.runtime.Token;
import org.ek9lang.compiler.symbols.ISymbol;

/**
 * TO be used when checking the left hand side and the right hand side with specific operators.
 */
public record TypeCompatibilityData(Token location, ISymbol lhs, ISymbol rhs) {
}
