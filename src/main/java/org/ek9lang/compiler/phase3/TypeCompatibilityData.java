package org.ek9lang.compiler.phase3;

import org.ek9lang.compiler.symbols.ISymbol;
import org.ek9lang.compiler.tokenizer.IToken;

/**
 * TO be used when checking the left hand side and the right hand side with specific operators.
 */
record TypeCompatibilityData(IToken location, ISymbol lhs, ISymbol rhs) {
}
